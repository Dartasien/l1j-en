-- IMPORTANT: THIS UPDATE IS NOT YET FINISHED, DONT USE!

-- Queries that are pending an update should be placed here. This allows them
-- to be verified as working together. Once complete, the -pending suffix will
-- be removed.

-- Migrate old tables still using MyISAM
ALTER TABLE accounts ENGINE='InnoDB';
ALTER TABLE area ENGINE='InnoDB';
ALTER TABLE armor ENGINE='InnoDB';
ALTER TABLE armor_set ENGINE='InnoDB';
ALTER TABLE ban_ip ENGINE='InnoDB';
ALTER TABLE beginner ENGINE='InnoDB';
ALTER TABLE board ENGINE='InnoDB';
ALTER TABLE board_auction ENGINE='InnoDB';
ALTER TABLE castle ENGINE='InnoDB';
ALTER TABLE character_buddys ENGINE='InnoDB';
ALTER TABLE character_buff ENGINE='InnoDB';
ALTER TABLE character_config ENGINE='InnoDB';
ALTER TABLE character_elf_warehouse ENGINE='InnoDB';
ALTER TABLE character_excludes ENGINE='InnoDB';
ALTER TABLE character_items ENGINE='InnoDB';
ALTER TABLE character_pvp ENGINE='InnoDB';
ALTER TABLE character_quests ENGINE='InnoDB';
ALTER TABLE character_skills ENGINE='InnoDB';
ALTER TABLE character_teleport ENGINE='InnoDB';
ALTER TABLE character_warehouse ENGINE='InnoDB';
ALTER TABLE characters ENGINE='InnoDB';
ALTER TABLE clan_data ENGINE='InnoDB';
ALTER TABLE clan_warehouse ENGINE='InnoDB';
ALTER TABLE commands ENGINE='InnoDB';
ALTER TABLE connection_test_table ENGINE='InnoDB';
ALTER TABLE door_gfxs ENGINE='InnoDB';
ALTER TABLE drop_item ENGINE='InnoDB';
ALTER TABLE droplist ENGINE='InnoDB';
ALTER TABLE dungeon ENGINE='InnoDB';
ALTER TABLE dungeon_random ENGINE='InnoDB';
ALTER TABLE etcitem ENGINE='InnoDB';
ALTER TABLE getback ENGINE='InnoDB';
ALTER TABLE getback_restart ENGINE='InnoDB';
ALTER TABLE house ENGINE='InnoDB';
ALTER TABLE inn ENGINE='InnoDB';
ALTER TABLE inn_key ENGINE='InnoDB';
ALTER TABLE letter ENGINE='InnoDB';
ALTER TABLE log_chat ENGINE='InnoDB';
ALTER TABLE log_enchant ENGINE='InnoDB';
ALTER TABLE mail ENGINE='InnoDB';
ALTER TABLE mapids ENGINE='InnoDB';
ALTER TABLE market_transactions ENGINE='InnoDB';
ALTER TABLE market_warehouse ENGINE='InnoDB';
ALTER TABLE mobgroup ENGINE='InnoDB';
ALTER TABLE mobskill ENGINE='InnoDB';
ALTER TABLE npc ENGINE='InnoDB';
ALTER TABLE npcaction ENGINE='InnoDB';
ALTER TABLE npcchat ENGINE='InnoDB';
ALTER TABLE petitem ENGINE='InnoDB';
ALTER TABLE pets ENGINE='InnoDB';
ALTER TABLE pettypes ENGINE='InnoDB';
ALTER TABLE polymorphs ENGINE='InnoDB';
ALTER TABLE quest_drops ENGINE='InnoDB';
ALTER TABLE resolvent ENGINE='InnoDB';
ALTER TABLE shop ENGINE='InnoDB';
ALTER TABLE skills ENGINE='InnoDB';
ALTER TABLE spawnlist ENGINE='InnoDB';
ALTER TABLE spawnlist_boss ENGINE='InnoDB';
ALTER TABLE spawnlist_door ENGINE='InnoDB';
ALTER TABLE spawnlist_furniture ENGINE='InnoDB';
ALTER TABLE spawnlist_light ENGINE='InnoDB';
ALTER TABLE spawnlist_npc ENGINE='InnoDB';
ALTER TABLE spawnlist_time ENGINE='InnoDB';
ALTER TABLE spawnlist_trap ENGINE='InnoDB';
ALTER TABLE spawnlist_ub ENGINE='InnoDB';
ALTER TABLE spr_action ENGINE='InnoDB';
ALTER TABLE town ENGINE='InnoDB';
ALTER TABLE trap ENGINE='InnoDB';
ALTER TABLE ub_managers ENGINE='InnoDB';
ALTER TABLE ub_settings ENGINE='InnoDB';
ALTER TABLE ub_times ENGINE='InnoDB';
ALTER TABLE weapon ENGINE='InnoDB';
ALTER TABLE weapon_skill ENGINE='InnoDB';

-- Make stackable Beast Tamer Ring.
update etcitem set stackable = 1 where item_id = 40454;

-- Make stackable Elementalist Ring.
update etcitem set stackable = 1 where item_id = 40465;

-- Make stackable Old Trading Document.
update etcitem set stackable = 1 where item_id = 40540;