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
package l1j.server.server.model;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A single enchant-proc tier loaded from the {@code enchant_proc} table: the
 * enchant level range it covers, its trigger probability, the damage it deals
 * on a trigger, and the effect that signifies the trigger.
 */
public class L1EnchantProcTier {
	private final int _tierId;
	private final int _minEnchant;
	private final int _maxEnchant;
	private final int _probability;
	private final String _damageType;
	private final int _minDamage;
	private final int _maxDamage;
	private final int _effectId;

	public L1EnchantProcTier(int tierId, int minEnchant, int maxEnchant,
			int probability, String damageType, int minDamage, int maxDamage,
			int effectId) {
		_tierId = tierId;
		_minEnchant = minEnchant;
		_maxEnchant = maxEnchant;
		_probability = probability;
		_damageType = damageType;
		_minDamage = minDamage;
		_maxDamage = maxDamage;
		_effectId = effectId;
	}

	public int getTierId() {
		return _tierId;
	}

	public int getMinEnchant() {
		return _minEnchant;
	}

	public int getMaxEnchant() {
		return _maxEnchant;
	}

	/**
	 * Trigger probability in percent (0-100). 0 is a valid "disabled tier":
	 * the tier loads but can never proc.
	 */
	public int getProbability() {
		return _probability;
	}

	public String getDamageType() {
		return _damageType;
	}

	public int getMinDamage() {
		return _minDamage;
	}

	public int getMaxDamage() {
		return _maxDamage;
	}

	public int getEffectId() {
		return _effectId;
	}

	/**
	 * Rolls bonus damage for a triggered proc: a uniform value in
	 * {@code [minDamage, maxDamage]}. When {@code minDamage == maxDamage} the
	 * constant is returned. The span is computed in {@code long} so extreme
	 * content values cannot overflow the random bound; an inverted range
	 * (min > max, rejected by the loader) degrades to {@code minDamage}.
	 */
	public int rollDamage() {
		long span = (long) _maxDamage - _minDamage;
		if (span < 0) {
			span = 0;
		}
		return _minDamage + (int) ThreadLocalRandom.current().nextLong(span + 1);
	}
}
