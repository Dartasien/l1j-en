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
	private static EnchantProcTable _instance;
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
		} catch (SQLException e) {
			_log.error("error while creating enchant_proc table", e);
		} finally {
			SQLUtil.close(rs);
			SQLUtil.close(pstm);
			SQLUtil.close(con);
		}
	}

	private void fillEnchantProcTable(ResultSet rs) throws SQLException {
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
				continue;
			}
			if (minDamage < 0 || maxDamage < 0) {
				_log.warn("skipping enchant_proc tier " + tierId + ": negative damage ("
						+ minDamage + "-" + maxDamage + ")");
				continue;
			}
			if (damageType == null || !KNOWN_DAMAGE_TYPES.contains(damageType)) {
				_log.warn("skipping enchant_proc tier " + tierId + ": unknown damage type '"
						+ damageType + "'");
				continue;
			}
			_tiers.add(new L1EnchantProcTier(tierId, minEnchant, maxEnchant,
					probability, damageType, minDamage, maxDamage, effectId));
		}
		_log.info("List of enchant proc tiers: " + _tiers.size() + " Loaded");
	}

	/**
	 * Returns the tier whose enchant range contains the given enchant level,
	 * or {@code null} if no tier matches (no proc). If ranges ever overlap,
	 * the first match in tier_id order wins.
	 */
	public L1EnchantProcTier getTier(int enchantLevel) {
		for (L1EnchantProcTier tier : _tiers) {
			if (enchantLevel >= tier.getMinEnchant()
					&& enchantLevel <= tier.getMaxEnchant()) {
				return tier;
			}
		}
		return null;
	}
}
