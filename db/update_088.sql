-- enchant-scaled weapon procs: real 2009 US client effect IDs (Story 1.3)
-- Replaces the placeholder effect_id = 0 seeded by update_087.sql.
-- Idempotent: safe to re-apply; only effect_id is touched, so operator-tuned
-- tier values (enchant range, probability, damage) are preserved.
-- Requires update_087.sql (creates enchant_proc). All non-effect_id seed
-- columns below must match update_087.sql exactly.
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (1, 1, 5, 25, 'physical', 3, 7, 10) ON DUPLICATE KEY UPDATE `effect_id` = 10;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (2, 6, 7, 25, 'physical', 5, 9, 1811) ON DUPLICATE KEY UPDATE `effect_id` = 1811;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (3, 8, 8, 25, 'physical', 7, 12, 1810) ON DUPLICATE KEY UPDATE `effect_id` = 1810;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (4, 9, 9, 25, 'physical', 9, 16, 2165) ON DUPLICATE KEY UPDATE `effect_id` = 2165;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (5, 10, 10, 25, 'physical', 12, 21, 3924) ON DUPLICATE KEY UPDATE `effect_id` = 3924;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (6, 11, 11, 25, 'physical', 16, 27, 1819) ON DUPLICATE KEY UPDATE `effect_id` = 1819;
INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES (7, 12, 255, 25, 'physical', 22, 35, 762) ON DUPLICATE KEY UPDATE `effect_id` = 762;
