Read `E:\git\l1j-en\.agents\skills\bmad-code-review\review-prompts\verification-gap.md` completely and follow it as your review instructions.

Review content:

diff --git a/db/update_087.sql b/db/update_087.sql
new file mode 100644
index 00000000..20d96503
--- /dev/null
+++ b/db/update_087.sql
@@ -0,0 +1,23 @@
+-- enchant-scaled weapon procs: tier definitions (Story 1.1)
+-- Tunable content: enchant range, trigger chance, damage range/type, signifying effect.
+-- effect_id is a placeholder (0) until Story 1.3 selects real 2009 US client effect IDs.
+CREATE TABLE IF NOT EXISTS `enchant_proc` (
+  `tier_id` int(11) unsigned NOT NULL AUTO_INCREMENT,
+  `min_enchant` int(11) NOT NULL DEFAULT '0',
+  `max_enchant` int(11) NOT NULL DEFAULT '0',
+  `probability` int(11) unsigned NOT NULL DEFAULT '0',
+  `damage_type` varchar(32) NOT NULL DEFAULT 'physical',
+  `min_damage` int(11) unsigned NOT NULL DEFAULT '0',
+  `max_damage` int(11) unsigned NOT NULL DEFAULT '0',
+  `effect_id` int(11) unsigned NOT NULL DEFAULT '0',
+  PRIMARY KEY (`tier_id`)
+);
+
+INSERT INTO `enchant_proc` (`min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES
+('1', '5', '25', 'physical', '3', '7', '0'),
+('6', '7', '25', 'physical', '5', '9', '0'),
+('8', '8', '25', 'physical', '7', '12', '0'),
+('9', '9', '25', 'physical', '9', '16', '0'),
+('10', '10', '25', 'physical', '12', '21', '0'),
+('11', '11', '25', 'physical', '16', '27', '0'),
+('12', '255', '25', 'physical', '22', '35', '0');
diff --git a/src/l1j/server/server/GameServerThread.java b/src/l1j/server/server/GameServerThread.java
index 753cb21c..32aff3a1 100644
--- a/src/l1j/server/server/GameServerThread.java
+++ b/src/l1j/server/server/GameServerThread.java
@@ -45,6 +45,7 @@ import l1j.server.server.datatables.ClanTable;
 import l1j.server.server.datatables.DoorTable;
 import l1j.server.server.datatables.DropItemTable;
 import l1j.server.server.datatables.DropTable;
+import l1j.server.server.datatables.EnchantProcTable;
 import l1j.server.server.datatables.FurnitureSpawnTable;
 import l1j.server.server.datatables.GetBackRestartTable;
 import l1j.server.server.datatables.GetBackTable;
@@ -295,6 +296,7 @@ public class GameServerThread {
 		GeneralThreadPool.getInstance();
 		ChatLogTable.getInstance();
 		WeaponSkillTable.getInstance();
+		EnchantProcTable.getInstance();
 		NpcActionTable.load();
 		GMCommandsConfig.load();
 		Getback.loadGetBack();
diff --git a/src/l1j/server/server/datatables/EnchantProcTable.java b/src/l1j/server/server/datatables/EnchantProcTable.java
new file mode 100644
index 00000000..ac8dbeca
--- /dev/null
+++ b/src/l1j/server/server/datatables/EnchantProcTable.java
@@ -0,0 +1,126 @@
+/*
+ * This program is free software; you can redistribute it and/or modify
+ * it under the terms of the GNU General Public License as published by
+ * the Free Software Foundation; either version 2, or (at your option)
+ * any later version.
+ *
+ * This program is distributed in the hope that it will be useful,
+ * but WITHOUT ANY WARRANTY; without even the implied warranty of
+ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
+ * GNU General Public License for more details.
+ *
+ * You should have received a copy of the GNU General Public License
+ * along with this program; if not, write to the Free Software
+ * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
+ * 02111-1307, USA.
+ *
+ * http://www.gnu.org/copyleft/gpl.html
+ */
+package l1j.server.server.datatables;
+
+import java.sql.Connection;
+import java.sql.PreparedStatement;
+import java.sql.ResultSet;
+import java.sql.SQLException;
+import java.util.ArrayList;
+import java.util.Collections;
+import java.util.HashSet;
+import java.util.List;
+import java.util.Set;
+
+import org.slf4j.Logger;
+import org.slf4j.LoggerFactory;
+
+import l1j.server.L1DatabaseFactory;
+import l1j.server.server.model.L1EnchantProcTier;
+import l1j.server.server.utils.SQLUtil;
+
+/**
+ * Boot-loaded, read-only table of enchant-proc tiers from the
+ * {@code enchant_proc} table. Malformed rows are skipped with a warning; the
+ * server boots regardless.
+ */
+public class EnchantProcTable {
+	private static Logger _log = LoggerFactory.getLogger(EnchantProcTable.class
+			.getName());
+	private static EnchantProcTable _instance;
+	private final List<L1EnchantProcTier> _tiers = new ArrayList<L1EnchantProcTier>();
+	private static final Set<String> KNOWN_DAMAGE_TYPES = Collections
+			.unmodifiableSet(new HashSet<String>(java.util.Arrays
+					.asList("physical")));
+
+	public static EnchantProcTable getInstance() {
+		if (_instance == null) {
+			_instance = new EnchantProcTable();
+		}
+		return _instance;
+	}
+
+	private EnchantProcTable() {
+		loadEnchantProc();
+	}
+
+	private void loadEnchantProc() {
+		Connection con = null;
+		PreparedStatement pstm = null;
+		ResultSet rs = null;
+		try {
+			con = L1DatabaseFactory.getInstance().getConnection();
+			pstm = con.prepareStatement("SELECT * FROM enchant_proc ORDER BY tier_id");
+			rs = pstm.executeQuery();
+			fillEnchantProcTable(rs);
+		} catch (SQLException e) {
+			_log.error("error while creating enchant_proc table", e);
+		} finally {
+			SQLUtil.close(rs);
+			SQLUtil.close(pstm);
+			SQLUtil.close(con);
+		}
+	}
+
+	private void fillEnchantProcTable(ResultSet rs) throws SQLException {
+		while (rs.next()) {
+			int tierId = rs.getInt("tier_id");
+			int minEnchant = rs.getInt("min_enchant");
+			int maxEnchant = rs.getInt("max_enchant");
+			int probability = rs.getInt("probability");
+			String damageType = rs.getString("damage_type");
+			int minDamage = rs.getInt("min_damage");
+			int maxDamage = rs.getInt("max_damage");
+			int effectId = rs.getInt("effect_id");
+			if (minEnchant > maxEnchant) {
+				_log.warn("skipping enchant_proc tier " + tierId + ": min_enchant ("
+						+ minEnchant + ") > max_enchant (" + maxEnchant + ")");
+				continue;
+			}
+			if (minDamage < 0 || maxDamage < 0) {
+				_log.warn("skipping enchant_proc tier " + tierId + ": negative damage ("
+						+ minDamage + "-" + maxDamage + ")");
+				continue;
+			}
+			if (damageType == null || !KNOWN_DAMAGE_TYPES.contains(damageType)) {
+				_log.warn("skipping enchant_proc tier " + tierId + ": unknown damage type '"
+						+ damageType + "'");
+				continue;
+			}
+			_tiers.add(new L1EnchantProcTier(tierId, minEnchant, maxEnchant,
+					probability, damageType, minDamage, maxDamage, effectId));
+		}
+		_log.info("List of enchant proc tiers: " + _tiers.size() + " Loaded");
+	}
+
+	/**
+	 * Returns the tier whose enchant range contains the given enchant level,
+	 * or {@code null} if no tier matches (no proc). If ranges ever overlap,
+	 * the first match in tier_id order wins.
+	 */
+	public L1EnchantProcTier getTier(int enchantLevel) {
+		for (L1EnchantProcTier tier : _tiers) {
+			if (enchantLevel >= tier.getMinEnchant()
+					&& enchantLevel <= tier.getMaxEnchant()) {
+				return tier;
+			}
+		}
+		return null;
+	}
+}
diff --git a/src/l1j/server/server/model/L1EnchantProcTier.java b/src/l1j/server/server/model/L1EnchantProcTier.java
new file mode 100644
index 00000000..241043ba
--- /dev/null
+++ b/src/l1j/server/server/model/L1EnchantProcTier.java
@@ -0,0 +1,80 @@
+/*
+ * This program is free software; you can redistribute it and/or modify
+ * it under the terms of the GNU General Public License as published by
+ * the Free Software Foundation; either version 2, or (at your option)
+ * any later version.
+ *
+ * This program is distributed in the hope that it will be useful,
+ * but WITHOUT ANY WARRANTY; without even the implied warranty of
+ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
+ * GNU General Public License for more details.
+ *
+ * You should have received a copy of the GNU General Public License
+ * along with this program; if not, write to the Free Software
+ * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
+ * 02111-1307, USA.
+ *
+ * http://www.gnu.org/copyleft/gpl.html
+ */
+package l1j.server.server.model;
+
+/**
+ * A single enchant-proc tier loaded from the {@code enchant_proc} table: the
+ * enchant level range it covers, its trigger probability, the damage it deals
+ * on a trigger, and the effect that signifies the trigger.
+ */
+public class L1EnchantProcTier {
+	private final int _tierId;
+	private final int _minEnchant;
+	private final int _maxEnchant;
+	private final int _probability;
+	private final String _damageType;
+	private final int _minDamage;
+	private final int _maxDamage;
+	private final int _effectId;
+
+	public L1EnchantProcTier(int tierId, int minEnchant, int maxEnchant,
+			int probability, String damageType, int minDamage, int maxDamage,
+			int effectId) {
+		_tierId = tierId;
+		_minEnchant = minEnchant;
+		_maxEnchant = maxEnchant;
+		_probability = probability;
+		_damageType = damageType;
+		_minDamage = minDamage;
+		_maxDamage = maxDamage;
+		_effectId = effectId;
+	}
+
+	public int getTierId() {
+		return _tierId;
+	}
+
+	public int getMinEnchant() {
+		return _minEnchant;
+	}
+
+	public int getMaxEnchant() {
+		return _maxEnchant;
+	}
+
+	public int getProbability() {
+		return _probability;
+	}
+
+	public String getDamageType() {
+		return _damageType;
+	}
+
+	public int getMinDamage() {
+		return _minDamage;
+	}
+
+	public int getMaxDamage() {
+		return _maxDamage;
+	}
+
+	public int getEffectId() {
+		return _effectId;
+	}
+}
+++ b/db/update_087.sql
@@ -0,0 +1,23 @@
+-- enchant-scaled weapon procs: tier definitions (Story 1.1)
+-- Tunable content: enchant range, trigger chance, damage range/type, signifying effect.
+-- effect_id is a placeholder (0) until Story 1.3 selects real 2009 US client effect IDs.
+CREATE TABLE IF NOT EXISTS `enchant_proc` (
+  `tier_id` int(11) unsigned NOT NULL AUTO_INCREMENT,
+  `min_enchant` int(11) NOT NULL DEFAULT '0',
+  `max_enchant` int(11) NOT NULL DEFAULT '0',
+  `probability` int(11) unsigned NOT NULL DEFAULT '0',
+  `damage_type` varchar(32) NOT NULL DEFAULT 'physical',
+  `min_damage` int(11) unsigned NOT NULL DEFAULT '0',
+  `max_damage` int(11) unsigned NOT NULL DEFAULT '0',
+  `effect_id` int(11) unsigned NOT NULL DEFAULT '0',
+  PRIMARY KEY (`tier_id`)
+);
+
+INSERT INTO `enchant_proc` (`min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES
+('1', '5', '25', 'physical', '3', '7', '0'),
+('6', '7', '25', 'physical', '5', '9', '0'),
+('8', '8', '25', 'physical', '7', '12', '0'),
+('9', '9', '25', 'physical', '9', '16', '0'),
+('10', '10', '25', 'physical', '12', '21', '0'),
+('11', '11', '25', 'physical', '16', '27', '0'),
+('12', '255', '25', 'physical', '22', '35', '0');
+++ b/src/l1j/server/server/datatables/EnchantProcTable.java
@@ -0,0 +1,126 @@
+/*
+ * This program is free software; you can redistribute it and/or modify
+ * it under the terms of the GNU General Public License as published by
+ * the Free Software Foundation; either version 2, or (at your option)
+ * any later version.
+ *
+ * This program is distributed in the hope that it will be useful,
+ * but WITHOUT ANY WARRANTY; without even the implied warranty of
+ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
+ * GNU General Public License for more details.
+ *
+ * You should have received a copy of the GNU General Public License
+ * along with this program; if not, write to the Free Software
+ * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
+ * 02111-1307, USA.
+ *
+ * http://www.gnu.org/copyleft/gpl.html
+ */
+package l1j.server.server.datatables;
+
+import java.sql.Connection;
+import java.sql.PreparedStatement;
+import java.sql.ResultSet;
+import java.sql.SQLException;
+import java.util.ArrayList;
+import java.util.Collections;
+import java.util.HashSet;
+import java.util.List;
+import java.util.Set;
+
+import org.slf4j.Logger;
+import org.slf4j.LoggerFactory;
+
+import l1j.server.L1DatabaseFactory;
+import l1j.server.server.model.L1EnchantProcTier;
+import l1j.server.server.utils.SQLUtil;
+
+/**
+ * Boot-loaded, read-only table of enchant-proc tiers from the
+ * {@code enchant_proc} table. Malformed rows are skipped with a warning; the
+ * server boots regardless.
+ */
+public class EnchantProcTable {
+	private static Logger _log = LoggerFactory.getLogger(EnchantProcTable.class
+			.getName());
+	private static EnchantProcTable _instance;
+	private final List<L1EnchantProcTier> _tiers = new ArrayList<L1EnchantProcTier>();
+	private static final Set<String> KNOWN_DAMAGE_TYPES = Collections
+			.unmodifiableSet(new HashSet<String>(java.util.Arrays
+					.asList("physical")));
+
+	public static EnchantProcTable getInstance() {
+		if (_instance == null) {
+			_instance = new EnchantProcTable();
+		}
+		return _instance;
+	}
+
+	private EnchantProcTable() {
+		loadEnchantProc();
+	}
+
+	private void loadEnchantProc() {
+		Connection con = null;
+		PreparedStatement pstm = null;
+		ResultSet rs = null;
+		try {
+			con = L1DatabaseFactory.getInstance().getConnection();
+			pstm = con.prepareStatement("SELECT * FROM enchant_proc ORDER BY tier_id");
+			rs = pstm.executeQuery();
+			fillEnchantProcTable(rs);
+		} catch (SQLException e) {
+			_log.error("error while creating enchant_proc table", e);
+		} finally {
+			SQLUtil.close(rs);
+			SQLUtil.close(pstm);
+			SQLUtil.close(con);
+		}
+	}
+
+	private void fillEnchantProcTable(ResultSet rs) throws SQLException {
+		while (rs.next()) {
+			int tierId = rs.getInt("tier_id");
+			int minEnchant = rs.getInt("min_enchant");
+			int maxEnchant = rs.getInt("max_enchant");
+			int probability = rs.getInt("probability");
+			String damageType = rs.getString("damage_type");
+			int minDamage = rs.getInt("min_damage");
+			int maxDamage = rs.getInt("max_damage");
+			int effectId = rs.getInt("effect_id");
+			if (minEnchant > maxEnchant) {
+				_log.warn("skipping enchant_proc tier " + tierId + ": min_enchant ("
+						+ minEnchant + ") > max_enchant (" + maxEnchant + ")");
+				continue;
+			}
+			if (minDamage < 0 || maxDamage < 0) {
+				_log.warn("skipping enchant_proc tier " + tierId + ": negative damage ("
+						+ minDamage + "-" + maxDamage + ")");
+				continue;
+			}
+			if (damageType == null || !KNOWN_DAMAGE_TYPES.contains(damageType)) {
+				_log.warn("skipping enchant_proc tier " + tierId + ": unknown damage type '"
+						+ damageType + "'");
+				continue;
+			}
+			_tiers.add(new L1EnchantProcTier(tierId, minEnchant, maxEnchant,
+					probability, damageType, minDamage, maxDamage, effectId));
+		}
+		_log.info("List of enchant proc tiers: " + _tiers.size() + " Loaded");
+	}
+
+	/**
+	 * Returns the tier whose enchant range contains the given enchant level,
+	 * or {@code null} if no tier matches (no proc). If ranges ever overlap,
+	 * the first match in tier_id order wins.
+	 */
+	public L1EnchantProcTier getTier(int enchantLevel) {
+		for (L1EnchantProcTier tier : _tiers) {
+			if (enchantLevel >= tier.getMinEnchant()
+					&& enchantLevel <= tier.getMaxEnchant()) {
+				return tier;
+			}
+		}
+		return null;
+	}
+}
+++ b/src/l1j/server/server/model/L1EnchantProcTier.java
@@ -0,0 +1,80 @@
+/*
+ * This program is free software; you can redistribute it and/or modify
+ * it under the terms of the GNU General Public License as published by
+ * the Free Software Foundation; either version 2, or (at your option)
+ * any later version.
+ *
+ * This program is distributed in the hope that it will be useful,
+ * but WITHOUT ANY WARRANTY; without even the implied warranty of
+ * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
+ * GNU General Public License for more details.
+ *
+ * You should have received a copy of the GNU General Public License
+ * along with this program; if not, write to the Free Software
+ * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
+ * 02111-1307, USA.
+ *
+ * http://www.gnu.org/copyleft/gpl.html
+ */
+package l1j.server.server.model;
+
+/**
+ * A single enchant-proc tier loaded from the {@code enchant_proc} table: the
+ * enchant level range it covers, its trigger probability, the damage it deals
+ * on a trigger, and the effect that signifies the trigger.
+ */
+public class L1EnchantProcTier {
+	private final int _tierId;
+	private final int _minEnchant;
+	private final int _maxEnchant;
+	private final int _probability;
+	private final String _damageType;
+	private final int _minDamage;
+	private final int _maxDamage;
+	private final int _effectId;
+
+	public L1EnchantProcTier(int tierId, int minEnchant, int maxEnchant,
+			int probability, String damageType, int minDamage, int maxDamage,
+			int effectId) {
+		_tierId = tierId;
+		_minEnchant = minEnchant;
+		_maxEnchant = maxEnchant;
+		_probability = probability;
+		_damageType = damageType;
+		_minDamage = minDamage;
+		_maxDamage = maxDamage;
+		_effectId = effectId;
+	}
+
+	public int getTierId() {
+		return _tierId;
+	}
+
+	public int getMinEnchant() {
+		return _minEnchant;
+	}
+
+	public int getMaxEnchant() {
+		return _maxEnchant;
+	}
+
+	public int getProbability() {
+		return _probability;
+	}
+
+	public String getDamageType() {
+		return _damageType;
+	}
+
+	public int getMinDamage() {
+		return _minDamage;
+	}
+
+	public int getMaxDamage() {
+		return _maxDamage;
+	}
+
+	public int getEffectId() {
+		return _effectId;
+	}
+}

Do not invoke any skill. If the instruction file is unreadable, report that exact failure and stop. Return only the review result.
