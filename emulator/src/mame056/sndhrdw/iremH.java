/**
 * Ported to 0.56
 */
/**
 * Changelog
 * ---------
 * 07/05/2019 - ported to 0.56 (shadow)
 */
package mame056.sndhrdw;

import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;

import static mame056.sndintrfH.*;

import static mame056.sndhrdw.irem.*;

public class iremH {

    public static MachineCPU IREM_AUDIO_CPU = new MachineCPU(
            CPU_M6803 | CPU_AUDIO_CPU,
            3579545 / 4,
            irem_sound_readmem, irem_sound_writemem,
            irem_sound_readport, irem_sound_writeport,
            null, 0
    );

    public static MachineSound[] IREM_AUDIO = new MachineSound[]{
        new MachineSound(SOUND_AY8910,
        irem_ay8910_interface
        ),
        new MachineSound(SOUND_MSM5205,
        irem_msm5205_interface
        )
    };
}
