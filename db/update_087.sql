-- enchant-scaled weapon procs: tier definitions (Story 1.1)
-- Tunable content: enchant range, trigger chance, damage range/type, signifying effect.
-- effect_id is a placeholder (0) until Story 1.3 selects real 2009 US client effect IDs.
-- Idempotent: safe to re-apply; re-applying resets tiers 1-7 to the seed values above.
CREATE TABLE IF NOT EXISTS `enchant_proc` (
  `tier_id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `min_enchant` int(11) NOT NULL DEFAULT '0',
  `max_enchant` int(11) NOT NULL DEFAULT '0',
  `probability` int(11) unsigned NOT NULL DEFAULT '0',
  `damage_type` varchar(32) NOT NULL DEFAULT 'physical',
  `min_damage` int(11) unsigned NOT NULL DEFAULT '0',
  `max_damage` int(11) unsigned NOT NULL DEFAULT '0',
  `effect_id` int(11) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`tier_id`)
);

INSERT INTO `enchant_proc` (`tier_id`, `min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES
('1', '1', '5', '25', 'physical', '3', '7', '0'),
('2', '6', '7', '25', 'physical', '5', '9', '0'),
('3', '8', '8', '25', 'physical', '7', '12', '0'),
('4', '9', '9', '25', 'physical', '9', '16', '0'),
('5', '10', '10', '25', 'physical', '12', '21', '0'),
('6', '11', '11', '25', 'physical', '16', '27', '0'),
('7', '12', '255', '25', 'physical', '22', '35', '0')
ON DUPLICATE KEY UPDATE
  `min_enchant` = VALUES(`min_enchant`),
  `max_enchant` = VALUES(`max_enchant`),
  `probability` = VALUES(`probability`),
  `damage_type` = VALUES(`damage_type`),
  `min_damage` = VALUES(`min_damage`),
  `max_damage` = VALUES(`max_damage`),
  `effect_id` = VALUES(`effect_id`);
