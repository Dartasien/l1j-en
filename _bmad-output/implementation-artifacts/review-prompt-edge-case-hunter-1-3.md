Read `E:\git\l1j-en\_bmad\render\bmad-build\l1j-en-b688dc9fe181\3d6c9a210e44049f9f98/review-prompts/edge-case-hunter.md` completely and follow it as your review instructions.

Review content:

diff --git a/src/l1j/server/server/model/L1Attack.java b/src/l1j/server/server/model/L1Attack.java
index 5301ecae..d14265c3 100644
--- a/src/l1j/server/server/model/L1Attack.java
+++ b/src/l1j/server/server/model/L1Attack.java
@@ -842,6 +842,13 @@ public class L1Attack {
 					&& enchantProcTier.getProbability() >= ThreadLocalRandom
 							.current().nextInt(100) + 1) {
 				damage += enchantProcTier.rollDamage();
+				if (enchantProcTier.getEffectId() > 0) {
+					if (_isArrowType) {
+						_pc.sendAndBroadcast(new S_UseAttackSkill(_pc, _targetId, enchantProcTier.getEffectId(), _targetX, _targetY, ActionCodes.ACTION_Attack, false));
+					} else {
+						_pc.sendAndBroadcast(new S_SkillSound(_targetId, enchantProcTier.getEffectId()));
+					}
+				}
 			}
 		}

+++ db/update_088.sql (new file)
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (1, 1, 5, 25, 'physical', 3, 7, 10) ON DUPLICATE KEY UPDATE `effect_id` = 10;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (2, 6, 7, 25, 'physical', 5, 9, 1811) ON DUPLICATE KEY UPDATE `effect_id` = 1811;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (3, 8, 8, 25, 'physical', 7, 12, 1810) ON DUPLICATE KEY UPDATE `effect_id` = 1810;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (4, 9, 9, 25, 'physical', 9, 16, 2165) ON DUPLICATE KEY UPDATE `effect_id` = 2165;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (5, 10, 10, 25, 'physical', 12, 21, 3924) ON DUPLICATE KEY UPDATE `effect_id` = 3924;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (6, 11, 11, 25, 'physical', 16, 27, 1819) ON DUPLICATE KEY UPDATE `effect_id` = 1819;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (7, 12, 127, 25, 'physical', 22, 35, 762) ON DUPLICATE KEY UPDATE `effect_id` = 762;