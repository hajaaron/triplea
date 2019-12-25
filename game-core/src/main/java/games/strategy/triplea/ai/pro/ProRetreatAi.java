package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.data.ProBattleResult;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.IBattle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * Pro retreat AI.
 *
 * <ol>
 *   <li>Consider whether submerging increases/decreases TUV swing
 *   <li>Consider what territory needs units when retreating
 * </ol>
 *
 * AFAIK there are 2 options available for maps (land battles): 1. air can retreat separately on an
 * amphib attack 2. non-amphib land can retreat separately So the result would be 4 situations: 1.
 * revised: you can't retreat anything on amphib 2. only air can retreat on amphib 3. only
 * non-amphib land can retreat on amphib 4. aa50: air and non-amphib land can retreat on amphib
 * Check by following TripleA.Constants -> TripleA.Properties statis get methods -> MustFightBattle
 * For sea battles you can have: 1. attacker retreats all units at end of battle 2. attacker
 * submerges sub at start or end of battle 3. defender submerges (or moves if Classic rules) sub at
 * start or end of battle
 */
class ProRetreatAi {

  private final ProOddsCalculator calc;

  ProRetreatAi(final ProAi ai) {
    calc = ai.getCalc();
  }

  Territory retreatQuery(
      final UUID battleId,
      final Territory battleTerritory,
      final Collection<Territory> possibleTerritories) {

    // Get battle data
    final GameData data = ProData.getData();
    final PlayerId player = ProData.getPlayer();
    final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    final IBattle battle = delegate.getBattleTracker().getPendingBattle(battleId);

    // Get units and determine if attacker
    final boolean isAttacker = player.equals(battle.getAttacker());
    final Collection<Unit> attackers = battle.getAttackingUnits();
    final Collection<Unit> defenders = battle.getDefendingUnits();

    // Calculate battle results
    final ProBattleResult result =
        calc.calculateBattleResultsNoSubmerge(
            battleTerritory, attackers, defenders, new HashSet<>());

    // Determine if it has a factory
    int isFactory = 0;
    if (ProMatches.territoryHasInfraFactoryAndIsLand().test(battleTerritory)) {
      isFactory = 1;
    }

    // Determine production value and if it is a capital
    int production = 0;
    int isCapital = 0;
    final TerritoryAttachment ta = TerritoryAttachment.get(battleTerritory);
    if (ta != null) {
      production = ta.getProduction();
      if (ta.isCapital()) {
        isCapital = 1;
      }
    }

    // Calculate current attack value
    double territoryValue = 0;
    if (result.isHasLandUnitRemaining() || attackers.stream().noneMatch(Matches.unitIsAir())) {
      territoryValue =
          result.getWinPercentage() / 100 * (2.0 * production * (1 + isFactory) * (1 + isCapital));
    }
    double battleValue = result.getTuvSwing() + territoryValue;
    if (!isAttacker) {
      battleValue = -battleValue;
    }

    // Decide if we should retreat
    if (battleValue < 0) {

      // Retreat to capital if available otherwise the territory with highest defense strength
      Territory retreatTerritory = null;
      double maxStrength = Double.NEGATIVE_INFINITY;
      final Territory myCapital =
          TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
      for (final Territory t : possibleTerritories) {
        if (t.equals(myCapital)) {
          retreatTerritory = t;
          break;
        }
        final double strength =
            ProBattleUtils.estimateStrength(
                t,
                t.getUnitCollection().getMatches(Matches.isUnitAllied(player, data)),
                new ArrayList<>(),
                false);
        if (strength > maxStrength) {
          retreatTerritory = t;
          maxStrength = strength;
        }
      }
      ProLogger.debug(
          player.getName()
              + " retreating from territory "
              + battleTerritory
              + " to "
              + retreatTerritory
              + " because AttackValue="
              + battleValue
              + ", TUVSwing="
              + result.getTuvSwing()
              + ", possibleTerritories="
              + possibleTerritories.size());
      return retreatTerritory;
    }
    ProLogger.debug(
        player.getName()
            + " not retreating from territory "
            + battleTerritory
            + " with AttackValue="
            + battleValue
            + ", TUVSwing="
            + result.getTuvSwing());

    return null;
  }
}
