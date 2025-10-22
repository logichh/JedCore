package com.jedk1.jedcore.util;

import com.jedk1.jedcore.ability.waterbending.WaterBlast;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.earthbending.Collapse;
import com.projectkorra.projectkorra.earthbending.CollapseWall;
import com.projectkorra.projectkorra.earthbending.RaiseEarth;
import com.projectkorra.projectkorra.earthbending.RaiseEarthWall;
import com.projectkorra.projectkorra.earthbending.Ripple;
import com.projectkorra.projectkorra.earthbending.Shockwave;
import com.projectkorra.projectkorra.firebending.Blaze;
import com.projectkorra.projectkorra.firebending.BlazeArc;
import com.projectkorra.projectkorra.firebending.BlazeRing;
import com.projectkorra.projectkorra.firebending.FireBlast;
import com.projectkorra.projectkorra.firebending.FireBlastCharged;
import com.projectkorra.projectkorra.waterbending.SurgeWall;
import com.projectkorra.projectkorra.waterbending.SurgeWave;
import com.projectkorra.projectkorra.waterbending.Torrent;
import com.projectkorra.projectkorra.waterbending.TorrentWave;
import com.projectkorra.projectkorra.waterbending.WaterSpout;
import com.projectkorra.projectkorra.waterbending.WaterSpoutWave;
import com.projectkorra.projectkorra.waterbending.ice.IceSpikeBlast;
import com.projectkorra.projectkorra.waterbending.ice.IceSpikePillar;
import com.projectkorra.projectkorra.waterbending.ice.IceSpikePillarField;

import java.util.HashMap;
import java.util.Map;

public class AbilitySelector {
    private static Map<String, CoreAbility> specialAbilities = new HashMap<>();

    static {
        specialAbilities.put("FireBlast", CoreAbility.getAbility(FireBlast.class));
        specialAbilities.put("FireBlastCharged", CoreAbility.getAbility(FireBlastCharged.class));
        specialAbilities.put("Blaze", CoreAbility.getAbility(Blaze.class));
        specialAbilities.put("BlazeArc", CoreAbility.getAbility(BlazeArc.class));
        specialAbilities.put("BlazeRing", CoreAbility.getAbility(BlazeRing.class));

        specialAbilities.put("WaterSpout", CoreAbility.getAbility(WaterSpout.class));
        specialAbilities.put("WaterSpoutWave", CoreAbility.getAbility(WaterSpoutWave.class));
        specialAbilities.put("Torrent", CoreAbility.getAbility(Torrent.class));
        specialAbilities.put("TorrentWave", CoreAbility.getAbility(TorrentWave.class));
        specialAbilities.put("SurgeWall", CoreAbility.getAbility(SurgeWall.class));
        specialAbilities.put("SurgeWave", CoreAbility.getAbility(SurgeWave.class));
        specialAbilities.put("IceSpike", CoreAbility.getAbility(IceSpikeBlast.class));
        specialAbilities.put("IceSpikeBlast", CoreAbility.getAbility(IceSpikeBlast.class));
        specialAbilities.put("IceSpikePillar", CoreAbility.getAbility(IceSpikePillar.class));
        specialAbilities.put("IceSpikePillarField", CoreAbility.getAbility(IceSpikePillarField.class));

        specialAbilities.put("Shockwave", CoreAbility.getAbility(Shockwave.class));
        specialAbilities.put("Ripple", CoreAbility.getAbility(Ripple.class));
        specialAbilities.put("RaiseEarth", CoreAbility.getAbility(RaiseEarth.class));
        specialAbilities.put("RaiseEarthWall", CoreAbility.getAbility(RaiseEarthWall.class));
        specialAbilities.put("Collapse", CoreAbility.getAbility(Collapse.class));
        specialAbilities.put("CollapseWall", CoreAbility.getAbility(CollapseWall.class));

        specialAbilities.put("WaterGimbal", CoreAbility.getAbility(WaterBlast.class));
    }

    public static CoreAbility getAbility(String abilityName) {
        CoreAbility ability = specialAbilities.get(abilityName);

        if (ability != null)
            return ability;

        return CoreAbility.getAbility(abilityName);
    }
}
