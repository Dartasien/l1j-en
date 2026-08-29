-- enchant-scaled weapon procs: tier definitions (Story 1.1)
-- Tunable content: enchant range, trigger chance, damage range/type, signifying effect.
-- effect_id is a placeholder (0) until Story 1.3 selects real 2009 US client effect IDs.
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

INSERT INTO `enchant_proc` (`min_enchant`, `max_enchant`, `probability`, `damage_type`, `min_damage`, `max_damage`, `effect_id`) VALUES
('1', '5', '25', 'physical', '3', '7', '0'),
('6', '7', '25', 'physical', '5', '9', '0'),
('8', '8', '25', 'physical', '7', '12', '0'),
('9', '9', '25', 'physical', '9', '16', '0'),
('10', '10', '25', 'physical', '12', '21', '0'),
('11', '11', '25', 'physical', '16', '27', '0'),
('12', '255', '25', 'physical', '22', '35', '0');
