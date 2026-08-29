Read `E:\git\l1j-en\_bmad\render\bmad-build\l1j-en-b688dc9fe181\3d6c9a210e44049f9f98\review-prompts\verification-gap.md` completely and follow it as your review instructions.

Review content:
# Diff since baseline 41df7f23 (Story 1.2)

diff --git a/config/altsettings.properties b/config/altsettings.properties
index c52a3a06..a82724c2 100644
--- a/config/altsettings.properties
+++ b/config/altsettings.properties
@@ -191,6 +191,9 @@ RandomizedBossSpawnFactor = .5
 # Use int in weapon proc calculations
 UseIntProcs = False
 
+# Enable enchant-tier procs (bonus damage on attack based on weapon enchant level)
+UseEnchantProcs = True
+
 # Allow the use of pine wands in safety zones
 UsePineInSafety = True
 
diff --git a/src/l1j/server/Config.java b/src/l1j/server/Config.java
index aea31de8..7fb6d8a3 100644
--- a/src/l1j/server/Config.java
+++ b/src/l1j/server/Config.java
@@ -318,6 +318,7 @@ public final class Config {
 	public static double RANDOMIZED_BOSS_SPAWN_FACTOR;
 	public static boolean ROYAL_LEVEL_DAMAGE;
 	public static boolean USE_INT_PROCS;
+	public static boolean USE_ENCHANT_PROCS;
 	public static boolean AUTO_STONE;
 
 	public static int MAX_PT;
@@ -953,6 +954,8 @@ public final class Config {
 					"RoyalLevelDamage", "False"));
 			USE_INT_PROCS = Boolean.parseBoolean(altSettings.getProperty(
 					"UseIntProcs", "False"));
+			USE_ENCHANT_PROCS = Boolean.parseBoolean(altSettings.getProperty(
+					"UseEnchantProcs", "False"));
 			AUTO_STONE = Boolean.parseBoolean(altSettings.getProperty(
 					"UseAutoStone", "False"));
 			USE_PINE_IN_SAFETY = Boolean.parseBoolean(altSettings.getProperty("UsePineInSafety", "True"));
diff --git a/src/l1j/server/server/model/L1Attack.java b/src/l1j/server/server/model/L1Attack.java
index 4af623d2..63b219dc 100644
--- a/src/l1j/server/server/model/L1Attack.java
+++ b/src/l1j/server/server/model/L1Attack.java
@@ -88,6 +88,7 @@ import l1j.server.Config;
 import l1j.server.server.ActionCodes;
 import l1j.server.server.controllers.BossEventController;
 import l1j.server.server.controllers.WarTimeController;
+import l1j.server.server.datatables.EnchantProcTable;
 import l1j.server.server.model.Instance.L1DollInstance;
 import l1j.server.server.model.Instance.L1ItemInstance;
 import l1j.server.server.model.Instance.L1NpcInstance;
@@ -830,6 +831,17 @@ public class L1Attack {
 			damage += calcAttrEnchantDmg();
 		}
 
+		if (Config.USE_ENCHANT_PROCS) {
+			L1EnchantProcTier enchantProcTier = EnchantProcTable.getInstance()
+					.getTier(_weaponEnchant);
+			if (enchantProcTier != null
+					&& "physical".equalsIgnoreCase(enchantProcTier.getDamageType())
+					&& enchantProcTier.getProbability() >= ThreadLocalRandom
+							.current().nextInt(100) + 1) {
+				damage += enchantProcTier.rollDamage();
+			}
+		}
+
 		if (_weaponType2 == WeaponType.Chainsword) {
 			if (_pc.isFoeSlayer()) {
 				if (_pc.hasSkillEffect(STATUS_WEAKNESS_EXPOSURE_LV3)) {
diff --git a/src/l1j/server/server/model/L1EnchantProcTier.java b/src/l1j/server/server/model/L1EnchantProcTier.java
index 955b0972..18b1429e 100644
--- a/src/l1j/server/server/model/L1EnchantProcTier.java
+++ b/src/l1j/server/server/model/L1EnchantProcTier.java
@@ -18,6 +18,8 @@
  */
 package l1j.server.server.model;
 
+import java.util.concurrent.ThreadLocalRandom;
+
 /**
  * A single enchant-proc tier loaded from the {@code enchant_proc} table: the
  * enchant level range it covers, its trigger probability, the damage it deals
@@ -81,4 +83,14 @@ public class L1EnchantProcTier {
 	public int getEffectId() {
 		return _effectId;
 	}
+
+	/**
+	 * Rolls bonus damage for a triggered proc: a uniform value in
+	 * {@code [minDamage, maxDamage]}. When {@code minDamage == maxDamage} the
+	 * constant is returned.
+	 */
+	public int rollDamage() {
+		return _minDamage
+				+ ThreadLocalRandom.current().nextInt(_maxDamage - _minDamage + 1);
+	}
 }

# Untracked: _bmad-output/implementation-artifacts/spec-1-2-enchant-tier-proc-triggers-physical-damage-on-attack.md (spec document, not code)
