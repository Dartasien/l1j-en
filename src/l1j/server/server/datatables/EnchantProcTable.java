/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l1j.server.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.L1EnchantProcTier;
import l1j.server.server.utils.SQLUtil;

/**
 * Boot-loaded, read-only table of enchant-proc tiers from the
 * {@code enchant_proc} table. Malformed rows are skipped with a warning; the
 * server boots regardless.
 */
public class EnchantProcTable {
	private static Logger _log = LoggerFactory.getLogger(EnchantProcTable.class
			.getName());
	private static volatile EnchantProcTable _instance;
	private final List<L1EnchantProcTier> _tiers = new ArrayList<L1EnchantProcTier>();
	private static final Set<String> KNOWN_DAMAGE_TYPES = Collections
			.unmodifiableSet(new HashSet<String>(java.util.Arrays
					.asList("physical")));

	public static EnchantProcTable getInstance() {
		if (_instance == null) {
			_instance = new EnchantProcTable();
		}
		return _instance;
	}

	private EnchantProcTable() {
		loadEnchantProc();
	}

	private void loadEnchantProc() {
		Connection con = null;
		PreparedStatement pstm = null;
		ResultSet rs = null;
		try {
			con = L1DatabaseFactory.getInstance().getConnection();
			pstm = con.prepareStatement("SELECT * FROM enchant_proc ORDER BY tier_id");
			rs = pstm.executeQuery();
			fillEnchantProcTable(rs);
		} catch (Exception e) {
			_log.error("error while loading enchant_proc table", e);
		} finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	private void fillEnchantProcTable(ResultSet rs) throws SQLException {
		int skipped = 0;
		while (rs.next()) {
			int tierId = rs.getInt("tier_id");
			int minEnchant = rs.getInt("min_enchant");
			int maxEnchant = rs.getInt("max_enchant");
			int probability = rs.getInt("probability");
			String damageType = rs.getString("damage_type");
			int minDamage = rs.getInt("min_damage");
			int maxDamage = rs.getInt("max_damage");
			int effectId = rs.getInt("effect_id");
			if (minEnchant > maxEnchant) {
				_log.warn("skipping enchant_proc tier " + tierId + ": min_enchant ("
						+ minEnchant + ") > max_enchant (" + maxEnchant + ")");
				skipped++;
				continue;
			}
			// Defense in depth only: the damage columns are unsigned, so the
			// database cannot store negative values.
			if (minDamage < 0 || maxDamage < 0) {
				_log.warn("skipping enchant_proc tier " + tierId + ": negative damage ("
						+ minDamage + "-" + maxDamage + ")");
				skipped++;
				continue;
			}
			if (minDamage > maxDamage) {
				_log.warn("skipping enchant_proc tier " + tierId + ": min_damage ("
						+ minDamage + ") > max_damage (" + maxDamage + ")");
				skipped++;
				continue;
			}
			if (probability > 100) {
				_log.warn("skipping enchant_proc tier " + tierId + ": probability ("
						+ probability + ") > 100");
				skipped++;
				continue;
			}
			String normalizedType = null;
			if (damageType != null) {
				damageType = damageType.trim();
				for (String known : KNOWN_DAMAGE_TYPES) {
					if (known.equalsIgnoreCase(damageType)) {
						normalizedType = known;
						break;
					}
				}
			}
			if (normalizedType == null) {
				_log.warn("skipping enchant_proc tier " + tierId + ": unknown damage type '"
						+ damageType + "'");
				skipped++;
				continue;
			}
			if (probability == 0) {
				_log.info("enchant_proc tier " + tierId
						+ " has probability 0 (disabled tier)");
			}
			_tiers.add(new L1EnchantProcTier(tierId, minEnchant, maxEnchant,
					probability, normalizedType, minDamage, maxDamage, effectId));
		}
		Collections.sort(_tiers, new Comparator<L1EnchantProcTier>() {
			public int compare(L1EnchantProcTier a, L1EnchantProcTier b) {
				return a.getTierId() - b.getTierId();
			}
		});
		if (_tiers.isEmpty()) {
			_log.warn("enchant_proc table is empty (" + skipped
					+ " rows skipped) - no enchant proc tiers active");
		} else {
			_log.info("List of enchant proc tiers: " + _tiers.size() + " Loaded ("
					+ skipped + " skipped)");
		}
		selfCheck();
	}

	/**
	 * Boot-time self-check of the lookup contract: tiers must be ordered by
	 * tier_id and every tier's own min_enchant must resolve to a tier. The
	 * probes are content-relative so the check survives content tuning.
	 */
	private void selfCheck() {
		for (int i = 1; i < _tiers.size(); i++) {
			if (_tiers.get(i - 1).getTierId() >= _tiers.get(i).getTierId()) {
				_log.error("enchant_proc self-check failed: tiers not in tier_id order");
				return;
			}
		}
		for (L1EnchantProcTier tier : _tiers) {
			if (getTier(tier.getMinEnchant()) == null) {
				_log.error("enchant_proc self-check failed: getTier("
						+ tier.getMinEnchant() + ") returned null for tier "
						+ tier.getTierId());
				return;
			}
		}
	}

	/**
	 * Returns the tier whose enchant range contains the given enchant level,
	 * or {@code null} if no tier matches (no proc). If ranges ever overlap,
	 * the first match in tier_id order wins.
	 */
	public L1EnchantProcTier getTier(int enchantLevel) {
		if (enchantLevel < 0) {
			return null;
		}
		for (L1EnchantProcTier tier : _tiers) {
			if (enchantLevel >= tier.getMinEnchant()
					&& enchantLevel <= tier.getMaxEnchant()) {
				return tier;
			}
		}
		return null;
	}
}
