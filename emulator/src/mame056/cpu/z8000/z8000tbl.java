/*****************************************************************************
 *
 *	 z8000tbl.c
 *	 Portable Z8000(2) emulator
 *	 Opcode table (including mnemonics) and initialization
 *
 *	 Copyright (c) 1998 Juergen Buchmueller, all rights reserved.
 *	 Bug fixes and MSB_FIRST compliance Ernesto Corvi.
 *
 *	 - This source code is released as freeware for non-commercial purposes.
 *	 - You are free to use and redistribute this code in modified or
 *	   unmodified form, provided you list me in the credits.
 *	 - If you modify this source code, you must add a notice to each modified
 *	   source file that it has been changed.  If you're a nice person, you
 *	   will clearly mark each change too.  :)
 *	 - If you wish to use this for commercial purposes, please contact me at
 *	   pullmoll@t-online.de
 *	 - The author of this copywritten work reserves the right to change the
 *     terms of its usage and license at any time, including retroactively
 *   - This entire notice must remain in the source code.
 *
 *****************************************************************************/

package mame056.cpu.z8000;

import mame056.cpu.z80.z80.opcode;
import static mame056.cpu.z8000.z8000.z8000_exec;
import static mame056.cpu.z8000.z8000cpuH.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static mame056.cpu.z8000.z8000.z8000_zsp;
import mame056.cpu.z8000.z8000cpuH.Z8000_exec;
import mame056.cpu.z8000.z8000cpuH.Z8000_init;

import static mame056.cpu.z8000.z8000ops.*;

public class z8000tbl {
    
    
    public Z8000_init table[] = {
        op0, op1, op2, op3, op4, op5, op6, op7, op8, op9, 
        op10, op11, op12, op13, op14, op15, op16, op17, op18, op19,
        op20, op21, op22, op23, op24, op25, op26, op27, op28, op29,
        op30, op31, op32, op33, op34, op35, op36, op37, op38, op39,
        op40, op41, op42, op43, op44, op45, op46, op47, op48, op49,
        op50, op51, op52, op53, op54, op55, op56, op57, op58, op59,
        op60, op61, op62, op63, op64, op65, op66, op67, op68, op69,
        op70, op71, op72, op73, op74, op75, op76, op77, op78, op79,
        op80, op81, op82, op83, op84, op85, op86, op87, op88, op89,
        op90, op91, op92, op93, op94, op95, op96, op97, op98, op99,
        
        op100, op101, op102, op103, op104, op105, op106, op107, op108, op109, 
        op110, op111, op112, op113, op114, op115, op116, op117, op118, op119,
        op120, op121, op122, op123, op124, op125, op126, op127, op128, op129,
        op130, op131, op132, op133, op134, op135, op136, op137, op138, op139,
        op140, op141, op142, op143, op144, op145, op146, op147, op148, op149,
        op150, op151, op152, op153, op154, op155, op156, op157, op158, op159,
        op160, op161, op162, op163, op164, op165, op166, op167, op168, op169,
        op170, op171, op172, op173, op174, op175, op176, op177, op178, op179,
        op180, op181, op182, op183, op184, op185, op186, op187, op188, op189,
        op190, op191, op192, op193, op194, op195, op196, op197, op198, op199,
        
        op200, op201, op202, op203, op204, op205, op206, op207, op208, op209, 
        op210, op211, op212, op213, op214, op215, op216, op217, op218, op219,
        op220, op221, op222, op223, op224, op225, op226, op227, op228, op229,
        op230, op231, op232, op233, op234, op235, op236, op237, op238, op239,
        op240, op241, op242, op243, op244, op245, op246, op247, op248, op249,
        op250, op251, op252, op253, op254, op255, op256, op257, op258, op259,
        op260, op261, op262, op263, op264, op265, op266, op267, op268, op269,
        op270, op271, op272, op273, op274, op275, op276, op277, op278, op279,
        op280, op281, op282, op283, op284, op285, op286, op287, op288, op289,
        op290, op291, op292, op293, op294, op295, op296, op297, op298, op299,
        
        op300, op301, op302, op303, op304, op305, op306, op307, op308, op309, 
        op310, op311, op312, op313, op314, op315, op316, op317, op318, op319,
        op320, op321, op322, op323, op324, op325, op326, op327, op328, op329,
        op330, op331, op332, op333, op334, op335, op336, op337, op338, op339,
        op340, op341, op342, op343, op344, op345, op346, op347, op348, op349,
        op350, op351, op352, op353, op354, op355, op356, op357, op358, op359,
        op360, op361, op362, op363, op364, op365, op366, op367, op368, op369,
        op370, op371, op372, op373, op374, op375, op376, op377, op378, op379,
        op380, op381, op382, op383, op384, op385, op386, op387, op388, op389,
        op390, op391, op392, op393, op394, op395, op396, op397, op398, op399,
        
        op400, op401, op402, op403, op404, op405, op406, op407, op408, op409, 
        op410, op411, op412, op413, op414, op415, op416, op417, op418, op419,
        op420, op421, op422, op423, op424, op425, op426, op427, op428, op429,
        op430, op431, op432, op433, op434, op435, op436, op437, op438, op439,
        op440, op441, op442, op443, op444, op445, op446, op447, op448, op449,
        op450, op451, op452, op453, op454, op455, op456, op457, op458, op459,
        op460, op461, op462, op463, op464, op465, op466, op467, op468, op469,
        op470, op471, op472, op473, op474, op475, op476, op477, op478, op479,
        op480, op481, op482, op483, op484, op485, op486, op487, op488, op489,
        op490, op491, op492, op493, op494, op495, op496, op497, op498, op499,
        
        op500, op501, op502, op503, op504, op505, op506, op507, op508, op509, 
        op510, op511
    };

    public void z8000_init()
    {
        int i;
            Z8000_init init;

            /* already initialized? */
            if( z8000_exec != null )
                    return;

            /* allocate the opcode execution and disassembler array */
            z8000_exec = new Z8000_exec[0x10000];
            if (z8000_exec == null)
            {
                    logerror("cannot allocate Z8000 execution table\n");
                    return;
            }

            /* set up the zero, sign, parity lookup table */
            for (i = 0; i < 256; i++)
                    z8000_zsp[i] = ((i == 0) ? F_Z : 0) |
                           ((i & 128)!=0 ? F_S : 0) |
                           ((((i>>7)^(i>>6)^(i>>5)^(i>>4)^(i>>3)^(i>>2)^(i>>1)^i) & 1)!=0 ? F_PV : 0);

        /* first set all 64K opcodes to invalid */
            z8000_exec = new Z8000_exec[0x10000];
            
            for (i = 0; i < 0x10000; i++)
            {
                    z8000_exec[i] = new Z8000_exec();
                    z8000_exec[i].opcode = zinvalid;
                    z8000_exec[i].cycles = 4;
                    z8000_exec[i].size	 = 1;
                    z8000_exec[i].dasm	 = ".word   %#w0";
            }
            
            int _init = 0;

        /* now decompose the initialization table */
            for (init = table[_init]; init.size != 0; _init++)
            {
                    for (i = init.beg; i <= init.end; i += init.step)
                    {
                            if (z8000_exec[i].opcode != zinvalid)
                                    logerror("Z8000 opcode %04x clash '%s'\n", i, z8000_exec[i].dasm);

                            z8000_exec[i].opcode = init.opcode;
                            z8000_exec[i].cycles = init.cycles;
                            z8000_exec[i].size	 = init.size;
                            z8000_exec[i].dasm	 = init.dasm;
                    }
            }
    }

    public static void z8000_deinit()
    {
            if (z8000_exec == null)
                    return;
            //free( z8000_exec );
            z8000_exec = null;
    }
    
    public static Z8000_init op0 = new Z8000_init(0x0000,0x000f, 1,2,  7,Z00_0000_dddd_imm8, 					 "addb    %rb3,%#b3");
    public static Z8000_init op1 = new Z8000_init(0x0010,0x00ff, 1,1,  7,Z00_ssN0_dddd,							 "addb    %rb3,@%rw2");
    public static Z8000_init op2 = new Z8000_init(0x0010,0x00ff, 1,1,  7,Z00_ssN0_dddd,							 "addb    %rb3,@%rw2");
    public static Z8000_init op3 = new Z8000_init(0x0100,0x010f, 1,2,  7,Z01_0000_dddd_imm16,					 "add     %rw3,%#w1");
    public static Z8000_init op4 = new Z8000_init(0x0110,0x01ff, 1,1,  7,Z01_ssN0_dddd,							 "add     %rw3,@%rw2");
    public static Z8000_init op5 = new Z8000_init(0x0200,0x020f, 1,2,  7,Z02_0000_dddd_imm8, 					 "subb    %rb3,%#b3");
    public static Z8000_init op6 = new Z8000_init(0x0210,0x02ff, 1,1,  7,Z02_ssN0_dddd,							 "subb    %rb3,@%rw2");
    public static Z8000_init op7 = new Z8000_init(0x0300,0x030f, 1,2,  7,Z03_0000_dddd_imm16,					 "sub     %rw3,%#w1");
    public static Z8000_init op8 = new Z8000_init(0x0310,0x03ff, 1,1,  7,Z03_ssN0_dddd,							 "sub     %rw3,@%rw2");
    public static Z8000_init op9 = new Z8000_init(0x0400,0x040f, 1,2,  7,Z04_0000_dddd_imm8, 					 "orb     %rb3,%#b3");
    public static Z8000_init op10 = new Z8000_init(0x0410,0x04ff, 1,1,  7,Z04_ssN0_dddd,							 "orb     %rb3,@%rw2");
    public static Z8000_init op11 = new Z8000_init(0x0500,0x050f, 1,2,  7,Z05_0000_dddd_imm16,					 "or      %rw3,%#w1");
    public static Z8000_init op12 = new Z8000_init(0x0510,0x05ff, 1,1,  7,Z05_ssN0_dddd,							 "or      %rw3,@%rw2");
    public static Z8000_init op13 = new Z8000_init(0x0600,0x060f, 1,2,  7,Z06_0000_dddd_imm8, 					 "andb    %rb3,%#b3");
    public static Z8000_init op14 = new Z8000_init(0x0610,0x06ff, 1,1,  7,Z06_ssN0_dddd,							 "andb    %rb3,@%rw2");
    public static Z8000_init op15 = new Z8000_init(0x0700,0x070f, 1,2,  7,Z07_0000_dddd_imm16,					 "and     %rw3,%#w1");
    public static Z8000_init op16 = new Z8000_init(0x0710,0x07ff, 1,1,  7,Z07_ssN0_dddd,							 "and     %rw3,@%rw2");
    public static Z8000_init op17 = new Z8000_init(0x0800,0x080f, 1,2,  7,Z08_0000_dddd_imm8, 					 "xorb    %rb3,%#b3");
    public static Z8000_init op18 = new Z8000_init(0x0810,0x08ff, 1,1,  7,Z08_ssN0_dddd,							 "xorb    %rb3,@%rw2");
    public static Z8000_init op19 = new Z8000_init(0x0900,0x090f, 1,2,  7,Z09_0000_dddd_imm16,					 "xor     %rw3,%#w1");
    public static Z8000_init op20 = new Z8000_init(0x0910,0x09ff, 1,1,  7,Z09_ssN0_dddd,							 "xor     %rw3,@%rw2");
    public static Z8000_init op21 = new Z8000_init(0x0a00,0x0a0f, 1,2,  7,Z0A_0000_dddd_imm8, 					 "cpb     %rb3,%#b3");
    public static Z8000_init op22 = new Z8000_init(0x0a10,0x0aff, 1,1,  7,Z0A_ssN0_dddd,							 "cpb     %rb3,@%rw2");
    public static Z8000_init op23 = new Z8000_init(0x0b00,0x0b0f, 1,2,  7,Z0B_0000_dddd_imm16,					 "cp      %rw3,%#w1");
    public static Z8000_init op24 = new Z8000_init(0x0b10,0x0bff, 1,1,  7,Z0B_ssN0_dddd,							 "cp      %rw3,@%rw2");
    public static Z8000_init op25 = new Z8000_init(0x0c10,0x0cf0,16,1, 12,Z0C_ddN0_0000,							 "comb    @%rw2");
    public static Z8000_init op26 = new Z8000_init(0x0c11,0x0cf1,16,2, 11,Z0C_ddN0_0001_imm8, 					 "cpb     @%rw2,%#b3");
    public static Z8000_init op27 = new Z8000_init(0x0c12,0x0cf2,16,1, 12,Z0C_ddN0_0010,							 "negb    @%rw2");
    public static Z8000_init op28 = new Z8000_init(0x0c14,0x0cf4,16,1,  8,Z0C_ddN0_0100,							 "testb   @%rw2");
    public static Z8000_init op29 = new Z8000_init(0x0c15,0x0cf5,16,2,  7,Z0C_ddN0_0101_imm8, 					 "ldb     @%rw2,%#b3");
    public static Z8000_init op30 = new Z8000_init(0x0c16,0x0cf6,16,1, 11,Z0C_ddN0_0110,							 "tsetb   @%rw2");
    public static Z8000_init op31 = new Z8000_init(0x0c18,0x0cf8,16,1,  8,Z0C_ddN0_1000,							 "clrb    @%rw2");
    public static Z8000_init op32 = new Z8000_init(0x0d10,0x0df0,16,1, 12,Z0D_ddN0_0000,							 "com     @%rw2");
    public static Z8000_init op33 = new Z8000_init(0x0d11,0x0df1,16,2, 11,Z0D_ddN0_0001_imm16,					 "cp      @%rw2,%#w1");
    public static Z8000_init op34 = new Z8000_init(0x0d12,0x0df2,16,1, 12,Z0D_ddN0_0010,							 "neg     @%rw2");
    public static Z8000_init op35 = new Z8000_init(0x0d14,0x0df4,16,1,  8,Z0D_ddN0_0100,							 "test    @%rw2");
    public static Z8000_init op36 = new Z8000_init(0x0d15,0x0df5,16,2, 11,Z0D_ddN0_0101_imm16,					 "ld      @%rw2,%#w1");  /* fix cycles ld IR,IM */
    public static Z8000_init op37 = new Z8000_init(0x0d16,0x0df6,16,1, 11,Z0D_ddN0_0110,							 "tset    @%rw2");
    public static Z8000_init op38 = new Z8000_init(0x0d18,0x0df8,16,1,  8,Z0D_ddN0_1000,							 "clr     @%rw2");
    public static Z8000_init op39 = new Z8000_init(0x0d19,0x0df9,16,2, 12,Z0D_ddN0_1001_imm16,					 "push    @%rw2,%#w1");
    public static Z8000_init op40 = new Z8000_init(0x0e00,0x0eff, 1,1, 10,Z0E_imm8,								 "ext0e   %#b1");
    public static Z8000_init op41 = new Z8000_init(0x0f00,0x0fff, 1,1, 10,Z0F_imm8,								 "ext0f   %#b1");
    public static Z8000_init op42 = new Z8000_init(0x1000,0x100f, 1,3, 14,Z10_0000_dddd_imm32,					 "cpl     %rl3,%#l1");
    public static Z8000_init op43 = new Z8000_init(0x1010,0x10ff, 1,1, 14,Z10_ssN0_dddd,							 "cpl     %rl3,@%rw2");
    public static Z8000_init op44 = new Z8000_init(0x1111,0x11ff, 1,1, 20,Z11_ddN0_ssN0,							 "pushl   @%rw2,@%rw3");
    public static Z8000_init op45 = new Z8000_init(0x1200,0x120f, 1,3, 14,Z12_0000_dddd_imm32,					 "subl    %rl3,%#l1");
    public static Z8000_init op46 = new Z8000_init(0x1210,0x12ff, 1,1, 14,Z12_ssN0_dddd,							 "subl    %rl3,@%rw2");
    public static Z8000_init op47 = new Z8000_init(0x1311,0x13ff, 1,1, 13,Z13_ddN0_ssN0,							 "push    @%rw2,@%rw3");
    public static Z8000_init op48 = new Z8000_init(0x1400,0x140f, 1,3, 11,Z14_0000_dddd_imm32,					 "ldl     %rl3,%#l1");
    public static Z8000_init op49 = new Z8000_init(0x1410,0x14ff, 1,1, 11,Z14_ssN0_dddd,							 "ldl     %rl3,@%rw2");
    public static Z8000_init op50 = new Z8000_init(0x1511,0x15ff, 1,1, 19,Z15_ssN0_ddN0,							 "popl    @%rw3,@%rw2");
    public static Z8000_init op51 = new Z8000_init(0x1600,0x160f, 1,3, 14,Z16_0000_dddd_imm32,					 "addl    %rl3,%#l1");
    public static Z8000_init op52 = new Z8000_init(0x1610,0x16ff, 1,1, 14,Z16_ssN0_dddd,							 "addl    %rl3,@%rw2");
    public static Z8000_init op53 = new Z8000_init(0x1711,0x17ff, 1,1, 12,Z17_ssN0_ddN0,							 "pop     @%rw3,@%rw2");
    public static Z8000_init op54 = new Z8000_init(0x1810,0x18ff, 1,1,282,Z18_ssN0_dddd,							 "multl   %rq3,@%rw2");
    public static Z8000_init op55 = new Z8000_init(0x1900,0x190f, 1,2, 70,Z19_0000_dddd_imm16,					 "mult    %rl3,%#w1");
    public static Z8000_init op56 = new Z8000_init(0x1910,0x19ff, 1,1, 70,Z19_ssN0_dddd,							 "mult    %rl3,@%rw2");
    public static Z8000_init op57 = new Z8000_init(0x1a00,0x1a0f, 1,3,744,Z1A_0000_dddd_imm32,					 "divl    %rq3,%#l1");
    public static Z8000_init op58 = new Z8000_init(0x1a10,0x1aff, 1,1,744,Z1A_ssN0_dddd,							 "divl    %rq3,@%rw2");
    public static Z8000_init op59 = new Z8000_init(0x1b00,0x1b0f, 1,2,107,Z1B_0000_dddd_imm16,					 "div     %rl3,%#w1");
    public static Z8000_init op60 = new Z8000_init(0x1b10,0x1bff, 1,1,107,Z1B_ssN0_dddd,							 "div     %rl3,@%rw2");
    public static Z8000_init op61 = new Z8000_init(0x1c11,0x1cf1,16,2, 11,Z1C_ssN0_0001_0000_dddd_0000_nmin1, 	 "ldm     %rw5,@%rw2,n");
    public static Z8000_init op62 = new Z8000_init(0x1c18,0x1cf8,16,1, 13,Z1C_ddN0_1000,							 "testl   @%rw2");
    public static Z8000_init op63 = new Z8000_init(0x1c19,0x1cf9,16,2, 11,Z1C_ddN0_1001_0000_ssss_0000_nmin1, 	 "ldm     @%rw2,%rw5,n");
    public static Z8000_init op64 = new Z8000_init(0x1d10,0x1dff, 1,1, 11,Z1D_ddN0_ssss,							 "ldl     @%rw2,%rl3");
    public static Z8000_init op65 = new Z8000_init(0x1e10,0x1eff, 1,1, 10,Z1E_ddN0_cccc,							 "jp      %c3,%rw2");
    public static Z8000_init op66 = new Z8000_init(0x1f10,0x1ff0,16,1, 10,Z1F_ddN0_0000,							 "call    %rw2");
    public static Z8000_init op67 = new Z8000_init(0x2010,0x20ff, 1,1,  7,Z20_ssN0_dddd,							 "ldb     %rb3,@%rw2");
    public static Z8000_init op68 = new Z8000_init(0x2100,0x210f, 1,2,  7,Z21_0000_dddd_imm16,					 "ld      %rw3,%#w1");
    public static Z8000_init op69 = new Z8000_init(0x2110,0x21ff, 1,1,  7,Z21_ssN0_dddd,							 "ld      %rw3,@%rw2");
    public static Z8000_init op70 = new Z8000_init(0x2200,0x220f, 1,2, 10,Z22_0000_ssss_0000_dddd_0000_0000,		 "resb    %rb5,%rw3");
    public static Z8000_init op71 = new Z8000_init(0x2210,0x22ff, 1,1, 11,Z22_ddN0_imm4,							 "resb    @%rw3,%3");
    public static Z8000_init op72 = new Z8000_init(0x2300,0x230f, 1,2, 10,Z23_0000_ssss_0000_dddd_0000_0000,		 "res     %rw5,%rw3");
    public static Z8000_init op73 = new Z8000_init(0x2310,0x23ff, 1,1, 11,Z23_ddN0_imm4,							 "res     @%rw3,%3");
    public static Z8000_init op74 = new Z8000_init(0x2400,0x240f, 1,2, 10,Z24_0000_ssss_0000_dddd_0000_0000,		 "setb    %rb5,%rw3");
    public static Z8000_init op75 = new Z8000_init(0x2410,0x24ff, 1,1, 11,Z24_ddN0_imm4,							 "setb    @%rw3,%3");
    public static Z8000_init op76 = new Z8000_init(0x2500,0x250f, 1,2, 10,Z25_0000_ssss_0000_dddd_0000_0000,		 "set     %rw5,%rw3");
    public static Z8000_init op77 = new Z8000_init(0x2510,0x25ff, 1,1, 11,Z25_ddN0_imm4,							 "set     @%rw3,%3");
    public static Z8000_init op78 = new Z8000_init(0x2600,0x260f, 1,2, 10,Z26_0000_ssss_0000_dddd_0000_0000,		 "bitb    %rb5,%rw3");
    public static Z8000_init op79 = new Z8000_init(0x2610,0x26ff, 1,1,  8,Z26_ddN0_imm4,							 "bitb    @%rw3,%3");
    public static Z8000_init op80 = new Z8000_init(0x2700,0x270f, 1,2, 10,Z27_0000_ssss_0000_dddd_0000_0000,		 "bit     %rw5,%rw3");
    public static Z8000_init op81 = new Z8000_init(0x2710,0x27ff, 1,1,  8,Z27_ddN0_imm4,							 "bit     @%rw2,%3");
    public static Z8000_init op82 = new Z8000_init(0x2810,0x28ff, 1,1, 11,Z28_ddN0_imm4m1,						 "incb    @%rw2,%+3");
    public static Z8000_init op83 = new Z8000_init(0x2910,0x29ff, 1,1, 11,Z29_ddN0_imm4m1,						 "inc     @%rw2,%+3");
    public static Z8000_init op84 = new Z8000_init(0x2a10,0x2aff, 1,1, 11,Z2A_ddN0_imm4m1,						 "decb    @%rw2,%+3");
    public static Z8000_init op85 = new Z8000_init(0x2b10,0x2bff, 1,1, 11,Z2B_ddN0_imm4m1,						 "dec     @%rw2,%+3");
    public static Z8000_init op86 = new Z8000_init(0x2c10,0x2cff, 1,1, 12,Z2C_ssN0_dddd,							 "exb     %rb3,@%rw2");
    public static Z8000_init op87 = new Z8000_init(0x2d10,0x2dff, 1,1, 12,Z2D_ssN0_dddd,							 "ex      %rw3,@%rw2");
    public static Z8000_init op88 = new Z8000_init(0x2e10,0x2eff, 1,1,  8,Z2E_ddN0_ssss,							 "ldb     @%rw2,%rb3");
    public static Z8000_init op89 = new Z8000_init(0x2f10,0x2fff, 1,1,  8,Z2F_ddN0_ssss,							 "ld      @%rw2,%rw3");
    public static Z8000_init op90 = new Z8000_init(0x3000,0x300f, 1,2, 14,Z30_0000_dddd_dsp16,					 "ldrb    %rb3,%p1");
    public static Z8000_init op91 = new Z8000_init(0x3010,0x30ff, 1,2, 14,Z30_ssN0_dddd_imm16,					 "ldb     %rb3,%rw2(%#w1)");
    public static Z8000_init op92 = new Z8000_init(0x3100,0x310f, 1,2, 14,Z31_0000_dddd_dsp16,					 "ldr     %rw3,%p1");
    public static Z8000_init op93 = new Z8000_init(0x3110,0x31ff, 1,2, 14,Z31_ssN0_dddd_imm16,					 "ld      %rw3,%rw2(%#w1)");
    public static Z8000_init op94 = new Z8000_init(0x3200,0x320f, 1,2, 14,Z32_0000_ssss_dsp16,					 "ldrb    %p1,%rb3");
    public static Z8000_init op95 = new Z8000_init(0x3210,0x32ff, 1,2, 14,Z32_ddN0_ssss_imm16,					 "ldb     %rw2(%#w1);%rb3");
    public static Z8000_init op96 = new Z8000_init(0x3300,0x330f, 1,2, 14,Z33_0000_ssss_dsp16,					 "ldr     %p1,%rw3");
    public static Z8000_init op97 = new Z8000_init(0x3310,0x33ff, 1,2, 14,Z33_ddN0_ssss_imm16,					 "ld      %rw2(%#w1);%rw3");
    public static Z8000_init op98 = new Z8000_init(0x3400,0x340f, 1,2, 15,Z34_0000_dddd_dsp16,					 "ldar    p%rw3,%p1");
    public static Z8000_init op99 = new Z8000_init(0x3410,0x34ff, 1,2, 15,Z34_ssN0_dddd_imm16,					 "lda     p%rw3,%rw2(%#w1)");
    public static Z8000_init op100 = new Z8000_init(0x3500,0x350f, 1,2, 17,Z35_0000_dddd_dsp16,					 "ldrl    %rl3,%p1");
    public static Z8000_init op101 = new Z8000_init(0x3510,0x35ff, 1,2, 17,Z35_ssN0_dddd_imm16,					 "ldl     %rl3,%rw2(%#w1)");
    public static Z8000_init op102 = new Z8000_init(0x3600,0x3600, 1,1,  2,Z36_0000_0000,							 "bpt");
    public static Z8000_init op103 = new Z8000_init(0x3601,0x36ff, 1,1, 10,Z36_imm8,								 "rsvd36");
    public static Z8000_init op104 = new Z8000_init(0x3700,0x370f, 1,2, 17,Z37_0000_ssss_dsp16,					 "ldrl    %p1,%rl3");
    public static Z8000_init op105 = new Z8000_init(0x3710,0x37ff, 1,2, 17,Z37_ddN0_ssss_imm16,					 "ldl     %rw2(%#w1);%rl3");
    public static Z8000_init op106 = new Z8000_init(0x3800,0x38ff, 1,1, 10,Z38_imm8,								 "rsvd38");
    public static Z8000_init op107 = new Z8000_init(0x3910,0x39f0,16,1, 12,Z39_ssN0_0000,							 "ldps    @%rw2");
    public static Z8000_init op108 = new Z8000_init(0x3a00,0x3af0,16,2, 21,Z3A_ssss_0000_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op109 = new Z8000_init(0x3a01,0x3af1,16,2, 21,Z3A_ssss_0001_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op110 = new Z8000_init(0x3a02,0x3af2,16,2, 21,Z3A_ssss_0010_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op111 = new Z8000_init(0x3a03,0x3af3,16,2, 21,Z3A_ssss_0011_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op112 = new Z8000_init(0x3a04,0x3af4,16,2, 10,Z3A_dddd_0100_imm16,					 "%R %rb2,%#w1");
    public static Z8000_init op113 = new Z8000_init(0x3a05,0x3af5,16,2, 10,Z3A_dddd_0101_imm16,					 "%R %rb2,%#w1");
    public static Z8000_init op114 = new Z8000_init(0x3a06,0x3af6,16,2, 12,Z3A_ssss_0110_imm16,					 "%R %#w1,%rb2");
    public static Z8000_init op115 = new Z8000_init(0x3a07,0x3af7,16,2, 12,Z3A_ssss_0111_imm16,					 "%R %#w1,%rb2");
    public static Z8000_init op116 = new Z8000_init(0x3a08,0x3af8,16,2, 21,Z3A_ssss_1000_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op117 = new Z8000_init(0x3a09,0x3af9,16,2, 21,Z3A_ssss_1001_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op118 = new Z8000_init(0x3a0a,0x3afa,16,2, 21,Z3A_ssss_1010_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op119 = new Z8000_init(0x3a0b,0x3afb,16,2, 21,Z3A_ssss_1011_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op120 = new Z8000_init(0x3b00,0x3bf0,16,2, 21,Z3B_ssss_0000_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op121 = new Z8000_init(0x3b01,0x3bf1,16,2, 21,Z3B_ssss_0001_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op122 = new Z8000_init(0x3b02,0x3bf2,16,2, 21,Z3B_ssss_0010_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op123 = new Z8000_init(0x3b03,0x3bf3,16,2, 21,Z3B_ssss_0011_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op124 = new Z8000_init(0x3b04,0x3bf4,16,2, 12,Z3B_dddd_0100_imm16,					 "%R %rw2,%#w1");
    public static Z8000_init op125 = new Z8000_init(0x3b05,0x3bf5,16,2, 12,Z3B_dddd_0101_imm16,					 "%R %rw2,%#w1");
    public static Z8000_init op126 = new Z8000_init(0x3b06,0x3bf6,16,2, 12,Z3B_ssss_0110_imm16,					 "%R %#w1,%rw2");
    public static Z8000_init op127 = new Z8000_init(0x3b07,0x3bf7,16,2, 12,Z3B_ssss_0111_imm16,					 "%R %#w1,%rw2");
    public static Z8000_init op128 = new Z8000_init(0x3b08,0x3bf8,16,2, 21,Z3B_ssss_1000_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op129 = new Z8000_init(0x3b09,0x3bf9,16,2, 21,Z3B_ssss_1001_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rw5");
    public static Z8000_init op130 = new Z8000_init(0x3b0a,0x3bfa,16,2, 21,Z3B_ssss_1010_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rb5");
    public static Z8000_init op131 = new Z8000_init(0x3b0b,0x3bfb,16,2, 21,Z3B_ssss_1011_0000_aaaa_dddd_x000,		 "%R @%rw6,@%rw2,%rb5");
    public static Z8000_init op132 = new Z8000_init(0x3c00,0x3cff, 1,1, 10,Z3C_ssss_dddd,							 "inb     %rb3,@%rw2");
    public static Z8000_init op133 = new Z8000_init(0x3d00,0x3dff, 1,1, 10,Z3D_ssss_dddd,							 "in      %rw3,@%rw2");
    public static Z8000_init op134 = new Z8000_init(0x3e00,0x3eff, 1,1, 12,Z3E_dddd_ssss,							 "outb    @%rw2,%rb3");
    public static Z8000_init op135 = new Z8000_init(0x3f00,0x3fff, 1,1, 12,Z3F_dddd_ssss,							 "out     @%rw2,%rw3");
    public static Z8000_init op136 = new Z8000_init(0x4000,0x400f, 1,2,  9,Z40_0000_dddd_addr, 					 "addb    %rb3,%a1");
    public static Z8000_init op137 = new Z8000_init(0x4010,0x40ff, 1,2, 10,Z40_ssN0_dddd_addr, 					 "addb    %rb3,%a1(%rw2)");
    public static Z8000_init op138 = new Z8000_init(0x4100,0x410f, 1,2,  9,Z41_0000_dddd_addr, 					 "add     %rw3,%a1");
    public static Z8000_init op139 = new Z8000_init(0x4110,0x41ff, 1,2, 10,Z41_ssN0_dddd_addr, 					 "add     %rw3,%a1(%rw2)");
    public static Z8000_init op140 = new Z8000_init(0x4200,0x420f, 1,2,  9,Z42_0000_dddd_addr, 					 "subb    %rb3,%a1");
    public static Z8000_init op141 = new Z8000_init(0x4210,0x42ff, 1,2, 10,Z42_ssN0_dddd_addr, 					 "subb    %rb3,%a1(%rw2)");
    public static Z8000_init op142 = new Z8000_init(0x4300,0x430f, 1,2,  9,Z43_0000_dddd_addr, 					 "sub     %rw3,%a1");
    public static Z8000_init op143 = new Z8000_init(0x4310,0x43ff, 1,2, 10,Z43_ssN0_dddd_addr, 					 "sub     %rw3,%a1(%rw2)");
    public static Z8000_init op144 = new Z8000_init(0x4400,0x440f, 1,2,  9,Z44_0000_dddd_addr, 					 "orb     %rb3,%a1");
    public static Z8000_init op145 = new Z8000_init(0x4410,0x44ff, 1,2, 10,Z44_ssN0_dddd_addr, 					 "orb     %rb3,%a1(%rw2)");
    public static Z8000_init op146 = new Z8000_init(0x4500,0x450f, 1,2,  9,Z45_0000_dddd_addr, 					 "or      %rw3,%a1");
    public static Z8000_init op147 = new Z8000_init(0x4510,0x45ff, 1,2, 10,Z45_ssN0_dddd_addr, 					 "or      %rw3,%a1(%rw2)");
    public static Z8000_init op148 = new Z8000_init(0x4600,0x460f, 1,2,  9,Z46_0000_dddd_addr, 					 "andb    %rb3,%a1");
    public static Z8000_init op149 = new Z8000_init(0x4610,0x46ff, 1,2, 10,Z46_ssN0_dddd_addr, 					 "andb    %rb3,%a1(%rw2)");
    public static Z8000_init op150 = new Z8000_init(0x4700,0x470f, 1,2,  9,Z47_0000_dddd_addr, 					 "and     %rw3,%a1");
    public static Z8000_init op151 = new Z8000_init(0x4710,0x47ff, 1,2, 10,Z47_ssN0_dddd_addr, 					 "and     %rw3,%a1(%rw2)");
    public static Z8000_init op152 = new Z8000_init(0x4800,0x480f, 1,2,  9,Z48_0000_dddd_addr, 					 "xorb    %rb3,%a1");
    public static Z8000_init op153 = new Z8000_init(0x4810,0x48ff, 1,2, 10,Z48_ssN0_dddd_addr, 					 "xorb    %rb3,%a1(%rw2)");
    public static Z8000_init op154 = new Z8000_init(0x4900,0x490f, 1,2,  9,Z49_0000_dddd_addr, 					 "xor     %rw3,%a1");
    public static Z8000_init op155 = new Z8000_init(0x4910,0x49ff, 1,2, 10,Z49_ssN0_dddd_addr, 					 "xor     %rw3,%a1(%rw2)");
    public static Z8000_init op156 = new Z8000_init(0x4a00,0x4a0f, 1,2,  9,Z4A_0000_dddd_addr, 					 "cpb     %rb3,%a1");
    public static Z8000_init op157 = new Z8000_init(0x4a10,0x4aff, 1,2, 10,Z4A_ssN0_dddd_addr, 					 "cpb     %rb3,%a1(%rw2)");
    public static Z8000_init op158 = new Z8000_init(0x4b00,0x4b0f, 1,2,  9,Z4B_0000_dddd_addr, 					 "cp      %rw3,%a1");
    public static Z8000_init op159 = new Z8000_init(0x4b10,0x4bff, 1,2, 10,Z4B_ssN0_dddd_addr, 					 "cp      %rw3,%a1(%rw2)");
    public static Z8000_init op160 = new Z8000_init(0x4c00,0x4c00, 1,2, 15,Z4C_0000_0000_addr, 					 "comb    %a1");
    public static Z8000_init op161 = new Z8000_init(0x4c01,0x4c01, 1,3, 14,Z4C_0000_0001_addr_imm8,				 "cpb     %a1,%#b3");
    public static Z8000_init op162 = new Z8000_init(0x4c02,0x4c02, 1,2, 15,Z4C_0000_0010_addr, 					 "negb    %a1");
    public static Z8000_init op163 = new Z8000_init(0x4c04,0x4c04, 1,2, 11,Z4C_0000_0100_addr, 					 "testb   %a1");
    public static Z8000_init op164 = new Z8000_init(0x4c05,0x4c05, 1,3, 14,Z4C_0000_0101_addr_imm8,				 "ldb     %a1,%#b3");
    public static Z8000_init op165 = new Z8000_init(0x4c06,0x4c06, 1,2, 14,Z4C_0000_0110_addr, 					 "tsetb   %a1");
    public static Z8000_init op166 = new Z8000_init(0x4c08,0x4c08, 1,2, 11,Z4C_0000_1000_addr, 					 "clrb    %a1");
    public static Z8000_init op167 = new Z8000_init(0x4c10,0x4cf0,16,2, 16,Z4C_ddN0_0000_addr, 					 "comb    %a1(%rw2)");
    public static Z8000_init op168 = new Z8000_init(0x4c11,0x4cf1,16,3, 15,Z4C_ddN0_0001_addr_imm8,				 "cpb     %a1(%rw2);%#b3");
    public static Z8000_init op169 = new Z8000_init(0x4c12,0x4cf2,16,2, 16,Z4C_ddN0_0010_addr, 					 "negb    %a1(%rw2)");
    public static Z8000_init op170 = new Z8000_init(0x4c14,0x4cf4,16,2, 12,Z4C_ddN0_0100_addr, 					 "testb   %a1(%rw2)");
    public static Z8000_init op171 = new Z8000_init(0x4c15,0x4cf5,16,3, 15,Z4C_ddN0_0101_addr_imm8,				 "ldb     %a1(%rw2);%#b3");
    public static Z8000_init op172 = new Z8000_init(0x4c16,0x4cf6,16,2, 15,Z4C_ddN0_0110_addr, 					 "tsetb   %a1(%rw2)");
    public static Z8000_init op173 = new Z8000_init(0x4c18,0x4cf8,16,2, 12,Z4C_ddN0_1000_addr, 					 "clrb    %a1(%rw2)");
    public static Z8000_init op174 = new Z8000_init(0x4d00,0x4d00, 1,2, 15,Z4D_0000_0000_addr, 					 "com     %a1");
    public static Z8000_init op175 = new Z8000_init(0x4d01,0x4d01, 1,3, 14,Z4D_0000_0001_addr_imm16,				 "cp      %a1,%#w2");
    public static Z8000_init op176 = new Z8000_init(0x4d02,0x4d02, 1,2, 15,Z4D_0000_0010_addr, 					 "neg     %a1");
    public static Z8000_init op177 = new Z8000_init(0x4d04,0x4d04, 1,2, 11,Z4D_0000_0100_addr, 					 "test    %a1");
    public static Z8000_init op178 = new Z8000_init(0x4d05,0x4d05, 1,3, 14,Z4D_0000_0101_addr_imm16,				 "ld      %a1,%#w2");
    public static Z8000_init op179 = new Z8000_init(0x4d06,0x4d06, 1,2, 14,Z4D_0000_0110_addr, 					 "tset    %a1");
    public static Z8000_init op180 = new Z8000_init(0x4d08,0x4d08, 1,2, 11,Z4D_0000_1000_addr, 					 "clr     %a1");
    public static Z8000_init op181 = new Z8000_init(0x4d10,0x4df0,16,2, 16,Z4D_ddN0_0000_addr, 					 "com     %a1(%rw2)");
    public static Z8000_init op182 = new Z8000_init(0x4d11,0x4df1,16,3, 15,Z4D_ddN0_0001_addr_imm16,				 "cp      %a1(%rw2);%#w2");
    public static Z8000_init op183 = new Z8000_init(0x4d12,0x4df2,16,2, 16,Z4D_ddN0_0010_addr, 					 "neg     %a1(%rw2)");
    public static Z8000_init op184 = new Z8000_init(0x4d14,0x4df4,16,2, 12,Z4D_ddN0_0100_addr, 					 "test    %a1(%rw2)");
    public static Z8000_init op185 = new Z8000_init(0x4d15,0x4df5,16,3, 15,Z4D_ddN0_0101_addr_imm16,				 "ld      %a1(%rw2);%#w2");
    public static Z8000_init op186 = new Z8000_init(0x4d16,0x4df6,16,2, 15,Z4D_ddN0_0110_addr, 					 "tset    %a1(%rw2)");
    public static Z8000_init op187 = new Z8000_init(0x4d18,0x4df8,16,2, 12,Z4D_ddN0_1000_addr, 					 "clr     %a1(%rw2)");
    public static Z8000_init op188 = new Z8000_init(0x4e11,0x4ef0,16,2, 12,Z4E_ddN0_ssN0_addr, 					 "ldb     %a1(%rw2);%rb3");
    public static Z8000_init op189 = new Z8000_init(0x5000,0x500f, 1,2, 15,Z50_0000_dddd_addr, 					 "cpl     %rl3,%a1");
    public static Z8000_init op190 = new Z8000_init(0x5010,0x50ff, 1,2, 16,Z50_ssN0_dddd_addr, 					 "cpl     %rl3,%a1(%rw2)");
    public static Z8000_init op191 = new Z8000_init(0x5110,0x51f0,16,2, 21,Z51_ddN0_0000_addr, 					 "pushl   @%rw2,%a1");
    public static Z8000_init op192 = new Z8000_init(0x5111,0x51f1,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op193 = new Z8000_init(0x5112,0x51f2,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op194 = new Z8000_init(0x5113,0x51f3,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op195 = new Z8000_init(0x5114,0x51f4,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op196 = new Z8000_init(0x5115,0x51f5,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op197 = new Z8000_init(0x5116,0x51f6,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op198 = new Z8000_init(0x5117,0x51f7,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op199 = new Z8000_init(0x5118,0x51f8,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op200 = new Z8000_init(0x5119,0x51f9,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op201 = new Z8000_init(0x511a,0x51fa,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op202 = new Z8000_init(0x511b,0x51fb,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op203 = new Z8000_init(0x511c,0x51fc,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op204 = new Z8000_init(0x511d,0x51fd,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op205 = new Z8000_init(0x511e,0x51fe,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op206 = new Z8000_init(0x511f,0x51ff,16,2, 21,Z51_ddN0_ssN0_addr, 					 "pushl   @%rw2,%a1(%rw3)");
    public static Z8000_init op207 = new Z8000_init(0x5200,0x520f, 1,2, 15,Z52_0000_dddd_addr, 					 "subl    %rl3,%a1");
    public static Z8000_init op208 = new Z8000_init(0x5210,0x52ff, 1,2, 16,Z52_ssN0_dddd_addr, 					 "subl    %rl3,%a1(%rw2)");
    public static Z8000_init op209 = new Z8000_init(0x5310,0x53f0,16,2, 14,Z53_ddN0_0000_addr, 					 "push    @%rw2,%a1");
    public static Z8000_init op210 = new Z8000_init(0x5311,0x53f1,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op211 = new Z8000_init(0x5312,0x53f2,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op212 = new Z8000_init(0x5313,0x53f3,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op213 = new Z8000_init(0x5314,0x53f4,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op214 = new Z8000_init(0x5315,0x53f5,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op215 = new Z8000_init(0x5316,0x53f6,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op216 = new Z8000_init(0x5317,0x53f7,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op217 = new Z8000_init(0x5318,0x53f8,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op218 = new Z8000_init(0x5319,0x53f9,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op219 = new Z8000_init(0x531a,0x53fa,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op220 = new Z8000_init(0x531b,0x53fb,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op221 = new Z8000_init(0x531c,0x53fc,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op222 = new Z8000_init(0x531d,0x53fd,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op223 = new Z8000_init(0x531e,0x53fe,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op224 = new Z8000_init(0x531f,0x53ff,16,2, 14,Z53_ddN0_ssN0_addr, 					 "push    @%rw2,%a1(%rw3)");
    public static Z8000_init op225 = new Z8000_init(0x5400,0x540f, 1,2, 12,Z54_0000_dddd_addr, 					 "ldl     %rl3,%a1");
    public static Z8000_init op226 = new Z8000_init(0x5410,0x54ff, 1,2, 13,Z54_ssN0_dddd_addr, 					 "ldl     %rl3,%a1(%rw2)");
    public static Z8000_init op227 = new Z8000_init(0x5510,0x55f0,16,2, 23,Z55_ssN0_0000_addr, 					 "popl    %a1,@%rw2");
    public static Z8000_init op228 = new Z8000_init(0x5511,0x55f1,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op229 = new Z8000_init(0x5512,0x55f2,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op230 = new Z8000_init(0x5513,0x55f3,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op231 = new Z8000_init(0x5514,0x55f4,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op232 = new Z8000_init(0x5515,0x55f5,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op233 = new Z8000_init(0x5516,0x55f6,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op234 = new Z8000_init(0x5517,0x55f7,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op235 = new Z8000_init(0x5518,0x55f8,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op236 = new Z8000_init(0x5519,0x55f9,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op237 = new Z8000_init(0x551a,0x55fa,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op238 = new Z8000_init(0x551b,0x55fb,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op239 = new Z8000_init(0x551c,0x55fc,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op240 = new Z8000_init(0x551d,0x55fd,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op241 = new Z8000_init(0x551e,0x55fe,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op242 = new Z8000_init(0x551f,0x55ff,16,2, 23,Z55_ssN0_ddN0_addr, 					 "popl    %a1(%rw3);@%rw2");
    public static Z8000_init op243 = new Z8000_init(0x5600,0x560f, 1,2, 15,Z56_0000_dddd_addr, 					 "addl    %rl3,%a1");
    public static Z8000_init op244 = new Z8000_init(0x5610,0x56ff, 1,2, 16,Z56_ssN0_dddd_addr, 					 "addl    %rl3,%a1(%rw2)");
    public static Z8000_init op245 = new Z8000_init(0x5710,0x57f0,16,2, 16,Z57_ssN0_0000_addr, 					 "pop     %a1,@%rw2");
    public static Z8000_init op246 = new Z8000_init(0x5711,0x57f1,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op247 = new Z8000_init(0x5712,0x57f2,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op248 = new Z8000_init(0x5713,0x57f3,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op249 = new Z8000_init(0x5714,0x57f4,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op250 = new Z8000_init(0x5715,0x57f5,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op251 = new Z8000_init(0x5716,0x57f6,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op252 = new Z8000_init(0x5717,0x57f7,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op253 = new Z8000_init(0x5718,0x57f8,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op254 = new Z8000_init(0x5719,0x57f9,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op255 = new Z8000_init(0x571a,0x57fa,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op256 = new Z8000_init(0x571b,0x57fb,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op257 = new Z8000_init(0x571c,0x57fc,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op258 = new Z8000_init(0x571d,0x57fd,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op259 = new Z8000_init(0x571e,0x57fe,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op260 = new Z8000_init(0x571f,0x57ff,16,2, 16,Z57_ssN0_ddN0_addr, 					 "pop     %a1(%rw3);@%rw2");
    public static Z8000_init op261 = new Z8000_init(0x5800,0x580f, 1,2,283,Z58_0000_dddd_addr, 					 "multl   %rq3,%a1");
    public static Z8000_init op262 = new Z8000_init(0x5810,0x58ff, 1,2,284,Z58_ssN0_dddd_addr, 					 "multl   %rq3,%a1(%rw2)");
    public static Z8000_init op263 = new Z8000_init(0x5900,0x590f, 1,2, 71,Z59_0000_dddd_addr, 					 "mult    %rl3,%a1");
    public static Z8000_init op264 = new Z8000_init(0x5910,0x59ff, 1,2, 72,Z59_ssN0_dddd_addr, 					 "mult    %rl3,%a1(%rw2)");
    public static Z8000_init op265 = new Z8000_init(0x5a00,0x5a0f, 1,2,745,Z5A_0000_dddd_addr, 					 "divl    %rq3,%a1");
    public static Z8000_init op266 = new Z8000_init(0x5a10,0x5aff, 1,2,746,Z5A_ssN0_dddd_addr, 					 "divl    %rq3,%a1(%rw2)");
    public static Z8000_init op267 = new Z8000_init(0x5b00,0x5b0f, 1,2,108,Z5B_0000_dddd_addr, 					 "div     %rl3,%a1");
    public static Z8000_init op268 = new Z8000_init(0x5b10,0x5bff, 1,2,109,Z5B_ssN0_dddd_addr, 					 "div     %rl3,%a1(%rw2)");
    public static Z8000_init op269 = new Z8000_init(0x5c01,0x5c01, 1,3, 14,Z5C_0000_0001_0000_dddd_0000_nmin1_addr, "ldm     %rw5,%a2,n");
    public static Z8000_init op270 = new Z8000_init(0x5c08,0x5c08, 1,2, 16,Z5C_0000_1000_addr, 					 "testl   %a1");
    public static Z8000_init op271 = new Z8000_init(0x5c09,0x5c09, 1,3, 14,Z5C_0000_1001_0000_ssss_0000_nmin1_addr, "ldm     %a2,%rw5,n");
    public static Z8000_init op272 = new Z8000_init(0x5c11,0x5cf1,16,3, 15,Z5C_ssN0_0001_0000_dddd_0000_nmin1_addr, "ldm     %rw5,%a2(%rw2);n");
    public static Z8000_init op273 = new Z8000_init(0x5c18,0x5cf8,16,2, 17,Z5C_ddN0_1000_addr, 					 "testl   %a1(%rw2)");
    public static Z8000_init op274 = new Z8000_init(0x5c19,0x5cf9,16,3, 15,Z5C_ddN0_1001_0000_ssN0_0000_nmin1_addr, "ldm     %a2(%rw2);%rw5,n");
    public static Z8000_init op275 = new Z8000_init(0x5d00,0x5d0f, 1,2, 15,Z5D_0000_ssss_addr, 					 "ldl     %a1,%rl3");
    public static Z8000_init op276 = new Z8000_init(0x5d10,0x5dff, 1,2, 14,Z5D_ddN0_ssss_addr, 					 "ldl     %a1(%rw2);%rl3");
    public static Z8000_init op277 = new Z8000_init(0x5e00,0x5e0f, 1,2,  7,Z5E_0000_cccc_addr, 					 "jp      %c3,%a1");
    public static Z8000_init op278 = new Z8000_init(0x5e10,0x5eff, 1,2,  8,Z5E_ddN0_cccc_addr, 					 "jp      %c3,%a1(%rw2)");
    public static Z8000_init op279 = new Z8000_init(0x5f00,0x5f00, 1,2, 12,Z5F_0000_0000_addr, 					 "call    %a1");
    public static Z8000_init op280 = new Z8000_init(0x5f10,0x5ff0,16,2, 13,Z5F_ddN0_0000_addr, 					 "call    %a1(%rw2)");
    public static Z8000_init op281 = new Z8000_init(0x6000,0x600f, 1,2,  9,Z60_0000_dddd_addr, 					 "ldb     %rb3,%a1");
    public static Z8000_init op282 = new Z8000_init(0x6010,0x60ff, 1,2, 10,Z60_ssN0_dddd_addr, 					 "ldb     %rb3,%a1(%rw2)");
    public static Z8000_init op283 = new Z8000_init(0x6100,0x610f, 1,2,  9,Z61_0000_dddd_addr, 					 "ld      %rw3,%a1");
    public static Z8000_init op284 = new Z8000_init(0x6110,0x61ff, 1,2, 10,Z61_ssN0_dddd_addr, 					 "ld      %rw3,%a1(%rw2)");
    public static Z8000_init op285 = new Z8000_init(0x6200,0x620f, 1,2, 13,Z62_0000_imm4_addr, 					 "resb    %a1,%3");
    public static Z8000_init op286 = new Z8000_init(0x6210,0x62ff, 1,2, 14,Z62_ddN0_imm4_addr, 					 "resb    %a1(%rw2);%3");
    public static Z8000_init op287 = new Z8000_init(0x6300,0x630f, 1,2, 13,Z63_0000_imm4_addr, 					 "res     %a1,%3");
    public static Z8000_init op288 = new Z8000_init(0x6310,0x63ff, 1,2, 14,Z63_ddN0_imm4_addr, 					 "res     %a1(%rw2);%3");
    public static Z8000_init op289 = new Z8000_init(0x6400,0x640f, 1,2, 13,Z64_0000_imm4_addr, 					 "setb    %a1,%3");
    public static Z8000_init op290 = new Z8000_init(0x6410,0x64ff, 1,2, 14,Z64_ddN0_imm4_addr, 					 "setb    %a1(%rw2);%3");
    public static Z8000_init op291 = new Z8000_init(0x6500,0x650f, 1,2, 13,Z65_0000_imm4_addr, 					 "set     %a1,%3");
    public static Z8000_init op292 = new Z8000_init(0x6510,0x65ff, 1,2, 14,Z65_ddN0_imm4_addr, 					 "set     %a1(%rw2);%3");
    public static Z8000_init op293 = new Z8000_init(0x6600,0x660f, 1,2, 10,Z66_0000_imm4_addr, 					 "bitb    %a1,%3");
    public static Z8000_init op294 = new Z8000_init(0x6610,0x66ff, 1,2, 11,Z66_ddN0_imm4_addr, 					 "bitb    %a1(%rw2);%3");
    public static Z8000_init op295 = new Z8000_init(0x6700,0x670f, 1,2, 10,Z67_0000_imm4_addr, 					 "bit     %a1,%3");
    public static Z8000_init op296 = new Z8000_init(0x6710,0x67ff, 1,2, 11,Z67_ddN0_imm4_addr, 					 "bit     %a1(%rw2);%3");
    public static Z8000_init op297 = new Z8000_init(0x6800,0x680f, 1,2, 13,Z68_0000_imm4m1_addr,					 "incb    %a1,%+3");
    public static Z8000_init op298 = new Z8000_init(0x6810,0x68ff, 1,2, 14,Z68_ddN0_imm4m1_addr,					 "incb    %a1(%rw2);%+3");
    public static Z8000_init op299 = new Z8000_init(0x6900,0x690f, 1,2, 13,Z69_0000_imm4m1_addr,					 "inc     %a1,%+3");
    public static Z8000_init op300 = new Z8000_init(0x6910,0x69ff, 1,2, 14,Z69_ddN0_imm4m1_addr,					 "inc     %a1(%rw2);%+3");
    public static Z8000_init op301 = new Z8000_init(0x6a00,0x6a0f, 1,2, 13,Z6A_0000_imm4m1_addr,					 "decb    %a1,%+3");
    public static Z8000_init op302 = new Z8000_init(0x6a10,0x6aff, 1,2, 14,Z6A_ddN0_imm4m1_addr,					 "decb    %a1(%rw2);%+3");
    public static Z8000_init op303 = new Z8000_init(0x6b00,0x6b0f, 1,2, 13,Z6B_0000_imm4m1_addr,					 "dec     %a1,%+3");
    public static Z8000_init op304 = new Z8000_init(0x6b10,0x6bff, 1,2, 14,Z6B_ddN0_imm4m1_addr,					 "dec     %a1(%rw2);%+3");
    public static Z8000_init op305 = new Z8000_init(0x6c00,0x6c0f, 1,2, 15,Z6C_0000_dddd_addr, 					 "exb     %rb3,%a1");
    public static Z8000_init op306 = new Z8000_init(0x6c10,0x6cff, 1,2, 16,Z6C_ssN0_dddd_addr, 					 "exb     %rb3,%a1(%rw2)");
    public static Z8000_init op307 = new Z8000_init(0x6d00,0x6d0f, 1,2, 15,Z6D_0000_dddd_addr, 					 "ex      %rw3,%a1");
    public static Z8000_init op308 = new Z8000_init(0x6d10,0x6dff, 1,2, 16,Z6D_ssN0_dddd_addr, 					 "ex      %rw3,%a1(%rw2)");
    public static Z8000_init op309 = new Z8000_init(0x6e00,0x6e0f, 1,2, 11,Z6E_0000_ssss_addr, 					 "ldb     %a1,%rb3");
    public static Z8000_init op310 = new Z8000_init(0x6e10,0x6eff, 1,2, 11,Z6E_ddN0_ssss_addr, 					 "ldb     %a1(%rw2);%rb3");
    public static Z8000_init op311 = new Z8000_init(0x6f00,0x6f0f, 1,2, 11,Z6F_0000_ssss_addr, 					 "ld      %a1,%rw3");
    public static Z8000_init op312 = new Z8000_init(0x6f10,0x6fff, 1,2, 12,Z6F_ddN0_ssss_addr, 					 "ld      %a1(%rw2);%rw3");
    public static Z8000_init op313 = new Z8000_init(0x7010,0x70ff, 1,2, 14,Z70_ssN0_dddd_0000_xxxx_0000_0000,		 "ldb     %rb3,%rw2(%rw5)");
    public static Z8000_init op314 = new Z8000_init(0x7110,0x71ff, 1,2, 14,Z71_ssN0_dddd_0000_xxxx_0000_0000,		 "ld      %rw3,%rw2(%rw5)");
    public static Z8000_init op315 = new Z8000_init(0x7210,0x72ff, 1,2, 14,Z72_ddN0_ssss_0000_xxxx_0000_0000,		 "ldb     %rw2(%rw5);%rb3");
    public static Z8000_init op316 = new Z8000_init(0x7310,0x73ff, 1,2, 14,Z73_ddN0_ssss_0000_xxxx_0000_0000,		 "ld      %rw2(%rw5);%rw3");
    public static Z8000_init op317 = new Z8000_init(0x7410,0x74ff, 1,2, 15,Z74_ssN0_dddd_0000_xxxx_0000_0000,		 "lda     p%rw3,%rw2(%rw5)");
    public static Z8000_init op318 = new Z8000_init(0x7510,0x75ff, 1,2, 17,Z75_ssN0_dddd_0000_xxxx_0000_0000,		 "ldl     %rl3,%rw2(%rw5)");
    public static Z8000_init op319 = new Z8000_init(0x7600,0x760f, 1,2, 12,Z76_0000_dddd_addr, 					 "lda     p%rw3,%a1");
    public static Z8000_init op320 = new Z8000_init(0x7610,0x76ff, 1,2, 13,Z76_ssN0_dddd_addr, 					 "lda     p%rw3,%a1(%rw2)");
    public static Z8000_init op321 = new Z8000_init(0x7710,0x77ff, 1,2, 17,Z77_ddN0_ssss_0000_xxxx_0000_0000,		 "ldl     %rw2(%rw5);%rl3");
    public static Z8000_init op322 = new Z8000_init(0x7800,0x78ff, 1,1, 10,Z78_imm8,								 "rsvd78");
    public static Z8000_init op323 = new Z8000_init(0x7900,0x7900, 1,2, 16,Z79_0000_0000_addr, 					 "ldps    %a1");
    public static Z8000_init op324 = new Z8000_init(0x7910,0x79f0,16,2, 17,Z79_ssN0_0000_addr, 					 "ldps    %a1(%rw2)");
    public static Z8000_init op325 = new Z8000_init(0x7a00,0x7a00, 1,1,  8,Z7A_0000_0000,							 "halt");
    public static Z8000_init op326 = new Z8000_init(0x7b00,0x7b00, 1,1, 13,Z7B_0000_0000,							 "iret");
    public static Z8000_init op327 = new Z8000_init(0x7b08,0x7b08, 1,1,  5,Z7B_0000_1000,							 "mset");
    public static Z8000_init op328 = new Z8000_init(0x7b09,0x7b09, 1,1,  5,Z7B_0000_1001,							 "mres");
    public static Z8000_init op329 = new Z8000_init(0x7b0a,0x7b0a, 1,1,  7,Z7B_0000_1010,							 "mbit");
    public static Z8000_init op330 = new Z8000_init(0x7b0d,0x7bfd,16,1, 12,Z7B_dddd_1101,							 "mreq    %rw2");
    public static Z8000_init op331 = new Z8000_init(0x7c00,0x7c03, 1,1,  7,Z7C_0000_00ii,							 "di      %i3");
    public static Z8000_init op332 = new Z8000_init(0x7c04,0x7c07, 1,1,  7,Z7C_0000_01ii,							 "ei      %i3");
    public static Z8000_init op333 = new Z8000_init(0x7d00,0x7df0,16,1,  7,Z7D_dddd_0ccc,							 "ldctl   %rw2,ctrl0");
    public static Z8000_init op334 = new Z8000_init(0x7d01,0x7df1,16,1,  7,Z7D_dddd_0ccc,							 "ldctl   %rw2,ctrl1");
    public static Z8000_init op335 = new Z8000_init(0x7d02,0x7df2,16,1,  7,Z7D_dddd_0ccc,							 "ldctl   %rw2,fcw");
    public static Z8000_init op336 = new Z8000_init(0x7d03,0x7df3,16,1,  7,Z7D_dddd_0ccc,							 "ldctl   %rw2,refresh");
    public static Z8000_init op337 = new Z8000_init(0x7d04,0x7df4,16,1,  7,Z7D_dddd_0ccc,							 "ldctl   %rw2,ctrl4");
    public static Z8000_init op338 = new Z8000_init(0x7d05,0x7df5,16,1,  7,Z7D_dddd_0ccc,							 "ldctl   %rw2,psap");
    public static Z8000_init op339 = new Z8000_init(0x7d06,0x7df6,16,1,  7,Z7D_dddd_0ccc,							 "ldctl   %rw2,ctrl6");
    public static Z8000_init op340 = new Z8000_init(0x7d07,0x7df7,16,1,  7,Z7D_dddd_0ccc,							 "ldctl   %rw2,nsp");
    public static Z8000_init op341 = new Z8000_init(0x7d08,0x7df8,16,1,  7,Z7D_ssss_1ccc,							 "ldctl   ctrl0,%rw2");
    public static Z8000_init op342 = new Z8000_init(0x7d09,0x7df9,16,1,  7,Z7D_ssss_1ccc,							 "ldctl   ctrl1,%rw2");
    public static Z8000_init op343 = new Z8000_init(0x7d0a,0x7dfa,16,1,  7,Z7D_ssss_1ccc,							 "ldctl   fcw,%rw2");
    public static Z8000_init op344 = new Z8000_init(0x7d0b,0x7dfb,16,1,  7,Z7D_ssss_1ccc,							 "ldctl   refresh,%rw2");
    public static Z8000_init op345 = new Z8000_init(0x7d0c,0x7dfc,16,1,  7,Z7D_ssss_1ccc,							 "ldctl   ctrl4,%rw2");
    public static Z8000_init op346 = new Z8000_init(0x7d0d,0x7dfd,16,1,  7,Z7D_ssss_1ccc,							 "ldctl   psap,%rw2");
    public static Z8000_init op347 = new Z8000_init(0x7d0e,0x7dfe,16,1,  7,Z7D_ssss_1ccc,							 "ldctl   ctrl6,%rw2");
    public static Z8000_init op348 = new Z8000_init(0x7d0f,0x7dff,16,1,  7,Z7D_ssss_1ccc,							 "ldctl   nsp,%rw2");
    public static Z8000_init op349 = new Z8000_init(0x7e00,0x7eff, 1,1, 10,Z7E_imm8,								 "rsvd7e  %#b1");
    public static Z8000_init op350 = new Z8000_init(0x7f00,0x7fff, 1,1, 33,Z7F_imm8,								 "sc      %#b1");
    public static Z8000_init op351 = new Z8000_init(0x8000,0x80ff, 1,1,  4,Z80_ssss_dddd,							 "addb    %rb3,%rb2");
    public static Z8000_init op352 = new Z8000_init(0x8100,0x81ff, 1,1,  4,Z81_ssss_dddd,							 "add     %rw3,%rw2");
    public static Z8000_init op353 = new Z8000_init(0x8200,0x82ff, 1,1,  4,Z82_ssss_dddd,							 "subb    %rb3,%rb2");
    public static Z8000_init op354 = new Z8000_init(0x8300,0x83ff, 1,1,  4,Z83_ssss_dddd,							 "sub     %rw3,%rw2");
    public static Z8000_init op355 = new Z8000_init(0x8400,0x84ff, 1,1,  4,Z84_ssss_dddd,							 "orb     %rb3,%rb2");
    public static Z8000_init op356 = new Z8000_init(0x8500,0x85ff, 1,1,  4,Z85_ssss_dddd,							 "or      %rw3,%rw2");
    public static Z8000_init op357 = new Z8000_init(0x8600,0x86ff, 1,1,  4,Z86_ssss_dddd,							 "andb    %rb3,%rb2");
    public static Z8000_init op358 = new Z8000_init(0x8700,0x87ff, 1,1,  4,Z87_ssss_dddd,							 "and     %rw3,%rw2");
    public static Z8000_init op359 = new Z8000_init(0x8800,0x88ff, 1,1,  4,Z88_ssss_dddd,							 "xorb    %rb3,%rb2");
    public static Z8000_init op360 = new Z8000_init(0x8900,0x89ff, 1,1,  4,Z89_ssss_dddd,							 "xor     %rw3,%rw2");
    public static Z8000_init op361 = new Z8000_init(0x8a00,0x8aff, 1,1,  4,Z8A_ssss_dddd,							 "cpb     %rb3,%rb2");
    public static Z8000_init op362 = new Z8000_init(0x8b00,0x8bff, 1,1,  4,Z8B_ssss_dddd,							 "cp      %rw3,%rw2");
    public static Z8000_init op363 = new Z8000_init(0x8c00,0x8cf0,16,1,  7,Z8C_dddd_0000,							 "comb    %rb2");
    public static Z8000_init op364 = new Z8000_init(0x8c02,0x8cf2,16,1,  7,Z8C_dddd_0010,							 "negb    %rb2");
    public static Z8000_init op365 = new Z8000_init(0x8c04,0x8cf4,16,1,  7,Z8C_dddd_0100,							 "testb   %rb2");
    public static Z8000_init op366 = new Z8000_init(0x8c06,0x8cf6,16,1,  7,Z8C_dddd_0110,							 "tsetb   %rb2");
    public static Z8000_init op367 = new Z8000_init(0x8c08,0x8cf8,16,1,  7,Z8C_dddd_1000,							 "clrb    %rb2");
    public static Z8000_init op368 = new Z8000_init(0x8d00,0x8df0,16,1,  7,Z8D_dddd_0000,							 "com     %rw2");
    public static Z8000_init op369 = new Z8000_init(0x8d01,0x8df1,16,1,  7,Z8D_imm4_0001,							 "setflg  %f2");
    public static Z8000_init op370 = new Z8000_init(0x8d02,0x8df2,16,1,  7,Z8D_dddd_0010,							 "neg     %rw2");
    public static Z8000_init op371 = new Z8000_init(0x8d03,0x8df3,16,1,  7,Z8D_imm4_0011,							 "resflg  %f2");
    public static Z8000_init op372 = new Z8000_init(0x8d04,0x8df4,16,1,  7,Z8D_dddd_0100,							 "test    %rw2");
    public static Z8000_init op373 = new Z8000_init(0x8d05,0x8df5,16,1,  7,Z8D_imm4_0101,							 "comflg  %f2");
    public static Z8000_init op374 = new Z8000_init(0x8d06,0x8df6,16,1,  7,Z8D_dddd_0110,							 "tset    %rw2");
    public static Z8000_init op375 = new Z8000_init(0x8d07,0x8d07, 1,1,  7,Z8D_0000_0111,							 "nop");
    public static Z8000_init op376 = new Z8000_init(0x8d08,0x8df8,16,1,  7,Z8D_dddd_1000,							 "clr     %rw2");
    public static Z8000_init op377 = new Z8000_init(0x8e00,0x8eff, 1,1, 10,Z8E_imm8,								 "ext8e   %#b1");
    public static Z8000_init op378 = new Z8000_init(0x8f00,0x8fff, 1,1, 10,Z8F_imm8,								 "ext8f   %#b1");
    public static Z8000_init op379 = new Z8000_init(0x9000,0x90ff, 1,1,  8,Z90_ssss_dddd,							 "cpl     %rl3,%rl2");
    public static Z8000_init op380 = new Z8000_init(0x9110,0x91ff, 1,1, 12,Z91_ddN0_ssss,							 "pushl   @%rw2,%rl3");
    public static Z8000_init op381 = new Z8000_init(0x9200,0x92ff, 1,1,  8,Z92_ssss_dddd,							 "subl    %rl3,%rl2");
    public static Z8000_init op382 = new Z8000_init(0x9310,0x93ff, 1,1,  9,Z93_ddN0_ssss,							 "push    @%rw2,%rw3");
    public static Z8000_init op383 = new Z8000_init(0x9400,0x94ff, 1,1,  5,Z94_ssss_dddd,							 "ldl     %rl3,%rl2");
    public static Z8000_init op384 = new Z8000_init(0x9510,0x95ff, 1,1, 12,Z95_ssN0_dddd,							 "popl    %rl3,@%rw2");
    public static Z8000_init op385 = new Z8000_init(0x9600,0x96ff, 1,1,  8,Z96_ssss_dddd,							 "addl    %rl3,%rl2");
    public static Z8000_init op386 = new Z8000_init(0x9710,0x97ff, 1,1,  8,Z97_ssN0_dddd,							 "pop     %rw3,@%rw2");
    public static Z8000_init op387 = new Z8000_init(0x9800,0x98ff, 1,1,282,Z98_ssss_dddd,							 "multl   %rq3,%rl2");
    public static Z8000_init op388 = new Z8000_init(0x9900,0x99ff, 1,1, 70,Z99_ssss_dddd,							 "mult    %rl3,%rw2");
    public static Z8000_init op389 = new Z8000_init(0x9a00,0x9aff, 1,1,744,Z9A_ssss_dddd,							 "divl    %rq3,%rl2");
    public static Z8000_init op390 = new Z8000_init(0x9b00,0x9bff, 1,1,107,Z9B_ssss_dddd,							 "div     %rl3,%rw2");
    public static Z8000_init op391 = new Z8000_init(0x9c08,0x9cf8,16,1, 13,Z9C_dddd_1000,							 "testl   %rl2");
    public static Z8000_init op392 = new Z8000_init(0x9d00,0x9dff, 1,1, 10,Z9D_imm8,								 "rsvd9d");
    public static Z8000_init op393 = new Z8000_init(0x9e00,0x9e0f, 1,1, 10,Z9E_0000_cccc,							 "ret     %c3");
    public static Z8000_init op394 = new Z8000_init(0x9f00,0x9fff, 1,1, 10,Z9F_imm8,								 "rsvd9f");
    public static Z8000_init op395 = new Z8000_init(0xa000,0xa0ff, 1,1,  3,ZA0_ssss_dddd,							 "ldb     %rb3,%rb2");
    public static Z8000_init op396 = new Z8000_init(0xa100,0xa1ff, 1,1,  3,ZA1_ssss_dddd,							 "ld      %rw3,%rw2");
    public static Z8000_init op397 = new Z8000_init(0xa200,0xa2ff, 1,1,  4,ZA2_dddd_imm4,							 "resb    %rb2,%3");
    public static Z8000_init op398 = new Z8000_init(0xa300,0xa3ff, 1,1,  4,ZA3_dddd_imm4,							 "res     %rw2,%3");
    public static Z8000_init op399 = new Z8000_init(0xa400,0xa4ff, 1,1,  4,ZA4_dddd_imm4,							 "setb    %rb2,%3");
    public static Z8000_init op400 = new Z8000_init(0xa500,0xa5ff, 1,1,  4,ZA5_dddd_imm4,							 "set     %rw2,%3");
    public static Z8000_init op401 = new Z8000_init(0xa600,0xa6ff, 1,1,  4,ZA6_dddd_imm4,							 "bitb    %rb2,%3");
    public static Z8000_init op402 = new Z8000_init(0xa700,0xa7ff, 1,1,  4,ZA7_dddd_imm4,							 "bit     %rw2,%3");
    public static Z8000_init op403 = new Z8000_init(0xa800,0xa8ff, 1,1,  4,ZA8_dddd_imm4m1,						 "incb    %rb2,%+3");
    public static Z8000_init op404 = new Z8000_init(0xa900,0xa9ff, 1,1,  4,ZA9_dddd_imm4m1,						 "inc     %rw2,%+3");
    public static Z8000_init op405 = new Z8000_init(0xaa00,0xaaff, 1,1,  4,ZAA_dddd_imm4m1,						 "decb    %rb2,%+3");
    public static Z8000_init op406 = new Z8000_init(0xab00,0xabff, 1,1,  4,ZAB_dddd_imm4m1,						 "dec     %rw2,%+3");
    public static Z8000_init op407 = new Z8000_init(0xac00,0xacff, 1,1,  6,ZAC_ssss_dddd,							 "exb     %rb3,%rb2");
    public static Z8000_init op408 = new Z8000_init(0xad00,0xadff, 1,1,  6,ZAD_ssss_dddd,							 "ex      %rw3,%rw2");
    public static Z8000_init op409 = new Z8000_init(0xae00,0xaeff, 1,1,  5,ZAE_dddd_cccc,							 "tccb    %c3,%rb2");
    public static Z8000_init op410 = new Z8000_init(0xaf00,0xafff, 1,1,  5,ZAF_dddd_cccc,							 "tcc     %c3,%rw2");
    public static Z8000_init op411 = new Z8000_init(0xb000,0xb0f0,16,1,  5,ZB0_dddd_0000,							 "dab     %rb2");
    public static Z8000_init op412 = new Z8000_init(0xb100,0xb1f0,16,1, 11,ZB1_dddd_0000,							 "extsb   %rw2");
    public static Z8000_init op413 = new Z8000_init(0xb107,0xb1f7,16,1, 11,ZB1_dddd_0111,							 "extsl   %rq2");
    public static Z8000_init op414 = new Z8000_init(0xb10a,0xb1fa,16,1, 11,ZB1_dddd_1010,							 "exts    %rl2");
    public static Z8000_init op415 = new Z8000_init(0xb200,0xb2f0,16,1,  6,ZB2_dddd_00I0,							 "rlb     %rb2,%?3");
    public static Z8000_init op416 = new Z8000_init(0xb201,0xb2f1,16,2, 13,ZB2_dddd_0001_imm8, 					 "s%*lb    %rb2,%$3");
    public static Z8000_init op417 = new Z8000_init(0xb202,0xb2f2,16,1,  6,ZB2_dddd_00I0,							 "rlb     %rb2,%?3");
    public static Z8000_init op418 = new Z8000_init(0xb203,0xb2f3,16,2, 15,ZB2_dddd_0011_0000_ssss_0000_0000,		 "sdlb    %rb2,%rw5");
    public static Z8000_init op419 = new Z8000_init(0xb204,0xb2f4,16,1,  6,ZB2_dddd_01I0,							 "rrb     %rb2,%?3");
    public static Z8000_init op420 = new Z8000_init(0xb206,0xb2f6,16,1,  6,ZB2_dddd_01I0,							 "rrb     %rb2,%?3");
    public static Z8000_init op421 = new Z8000_init(0xb208,0xb2f8,16,1,  9,ZB2_dddd_10I0,							 "rlcb    %rb2,%?3");
    public static Z8000_init op422 = new Z8000_init(0xb209,0xb2f9,16,2, 13,ZB2_dddd_1001_imm8, 					 "s%*ab    %rb2,%$3");
    public static Z8000_init op423 = new Z8000_init(0xb20a,0xb2fa,16,1,  9,ZB2_dddd_10I0,							 "rlcb    %rb2,%?3");
    public static Z8000_init op424 = new Z8000_init(0xb20b,0xb2fb,16,2, 15,ZB2_dddd_1011_0000_ssss_0000_0000,		 "sdab    %rb2,%rw5");
    public static Z8000_init op425 = new Z8000_init(0xb20c,0xb2fc,16,1,  9,ZB2_dddd_11I0,							 "rrcb    %rb2,%?3");
    public static Z8000_init op426 = new Z8000_init(0xb20e,0xb2fe,16,1,  9,ZB2_dddd_11I0,							 "rrcb    %rb2,%?3");
    public static Z8000_init op427 = new Z8000_init(0xb300,0xb3f0,16,1,  6,ZB3_dddd_00I0,							 "rl      %rw2,%?3");
    public static Z8000_init op428 = new Z8000_init(0xb301,0xb3f1,16,2, 13,ZB3_dddd_0001_imm8, 					 "s%*l     %rw2,%$3");
    public static Z8000_init op429 = new Z8000_init(0xb303,0xb3f3,16,2, 15,ZB3_dddd_0011_0000_ssss_0000_0000,		 "sdl     %rw2,%rw5");
    public static Z8000_init op430 = new Z8000_init(0xb304,0xb3f4,16,1,  6,ZB3_dddd_01I0,							 "rr      %rw2,%?3");
    public static Z8000_init op431 = new Z8000_init(0xb305,0xb3f5,16,2, 13,ZB3_dddd_0101_imm8, 					 "s%*ll    %rl2,%$3");
    public static Z8000_init op432 = new Z8000_init(0xb307,0xb3f7,16,2, 15,ZB3_dddd_0111_0000_ssss_0000_0000,		 "sdll    %rl2,%rw5");
    public static Z8000_init op433 = new Z8000_init(0xb308,0xb3f8,16,1,  6,ZB3_dddd_10I0,							 "rlc     %rw2,%?3");
    public static Z8000_init op434 = new Z8000_init(0xb309,0xb3f9,16,2, 13,ZB3_dddd_1001_imm8, 					 "s%*a     %rw2,%$3");
    public static Z8000_init op435 = new Z8000_init(0xb30b,0xb3fb,16,2, 15,ZB3_dddd_1011_0000_ssss_0000_0000,		 "sda     %rw2,%rw5");
    public static Z8000_init op436 = new Z8000_init(0xb30c,0xb3fc,16,1,  6,ZB3_dddd_11I0,							 "rrc     %rw2,%?3");
    public static Z8000_init op437 = new Z8000_init(0xb30d,0xb3fd,16,2, 13,ZB3_dddd_1101_imm8, 					 "s%*al    %rl2,%$3");
    public static Z8000_init op438 = new Z8000_init(0xb30f,0xb3ff,16,2, 15,ZB3_dddd_1111_0000_ssss_0000_0000,		 "sdal    %rl2,%rw5");
    public static Z8000_init op439 = new Z8000_init(0xb400,0xb4ff, 1,1,  5,ZB4_ssss_dddd,							 "adcb    %rb3,%rb2");
    public static Z8000_init op440 = new Z8000_init(0xb500,0xb5ff, 1,1,  5,ZB5_ssss_dddd,							 "adc     %rw3,%rw2");
    public static Z8000_init op441 = new Z8000_init(0xb600,0xb6ff, 1,1,  5,ZB6_ssss_dddd,							 "sbcb    %rb3,%rb2");
    public static Z8000_init op442 = new Z8000_init(0xb700,0xb7ff, 1,1,  5,ZB7_ssss_dddd,							 "sbc     %rw3,%rw2");
    public static Z8000_init op443 = new Z8000_init(0xb810,0xb8f0,16,2, 25,ZB8_ddN0_0000_0000_rrrr_ssN0_0000,		 "trib    @%rw2,@%rw6,rbr");
    public static Z8000_init op444 = new Z8000_init(0xb812,0xb8f2,16,2, 25,ZB8_ddN0_0010_0000_rrrr_ssN0_0000,		 "trtib   @%rw2,@%rw6,rbr");
    public static Z8000_init op445 = new Z8000_init(0xb814,0xb8f4,16,2, 25,ZB8_ddN0_0100_0000_rrrr_ssN0_0000,		 "trirb   @%rw2,@%rw6,rbr");
    public static Z8000_init op446 = new Z8000_init(0xb816,0xb8f6,16,2, 25,ZB8_ddN0_0110_0000_rrrr_ssN0_1110,		 "trtirb  @%rw2,@%rw6,rbr");
    public static Z8000_init op447 = new Z8000_init(0xb818,0xb8f8,16,2, 25,ZB8_ddN0_1000_0000_rrrr_ssN0_0000,		 "trdb    @%rw2,@%rw6,rbr");
    public static Z8000_init op448 = new Z8000_init(0xb81a,0xb8fa,16,2, 25,ZB8_ddN0_1010_0000_rrrr_ssN0_0000,		 "trtrb   @%rw2,@%rw6,rbr");
    public static Z8000_init op449 = new Z8000_init(0xb81c,0xb8fc,16,2, 25,ZB8_ddN0_1100_0000_rrrr_ssN0_0000,		 "trdrb   @%rw2,@%rw6,rbr");
    public static Z8000_init op450 = new Z8000_init(0xb81e,0xb8fe,16,2, 25,ZB8_ddN0_1110_0000_rrrr_ssN0_1110,		 "trtdrb  @%rw2,@%rw6,rbr");
    public static Z8000_init op451 = new Z8000_init(0xb900,0xb9ff,16,1, 10,ZB9_imm8,								 "rsvdb9");
    public static Z8000_init op452 = new Z8000_init(0xba10,0xbaf0,16,2, 11,ZBA_ssN0_0000_0000_rrrr_dddd_cccc,		 "cpib    %rb6,@%rw2,rr,%c7");
    public static Z8000_init op453 = new Z8000_init(0xba11,0xbaf1,16,2, 11,ZBA_ssN0_0001_0000_rrrr_ddN0_x000,		 "ldirb   @%rw6,@%rw2,rr");
    public static Z8000_init op454 = new Z8000_init(0xba12,0xbaf2,16,2, 11,ZBA_ssN0_0010_0000_rrrr_ddN0_cccc,		 "cpsib   @%rw6,@%rw2,rr,%c7");
    public static Z8000_init op455 = new Z8000_init(0xba14,0xbaf4,16,2, 11,ZBA_ssN0_0100_0000_rrrr_dddd_cccc,		 "cpirb   %rb6,@%rw2,rr,%c7");
    public static Z8000_init op456 = new Z8000_init(0xba16,0xbaf6,16,2, 11,ZBA_ssN0_0110_0000_rrrr_ddN0_cccc,		 "cpsirb  @%rw6,@%rw2,rr,%c7");
    public static Z8000_init op457 = new Z8000_init(0xba18,0xbaf8,16,2, 11,ZBA_ssN0_1000_0000_rrrr_dddd_cccc,		 "cpdb    %rb6,@%rw2,rr,%c7");
    public static Z8000_init op458 = new Z8000_init(0xba19,0xbaf9,16,2, 11,ZBA_ssN0_1001_0000_rrrr_ddN0_x000,		 "lddrb   @%rw2,@%rw6,rr");
    public static Z8000_init op459 = new Z8000_init(0xba1a,0xbafa,16,2, 11,ZBA_ssN0_1010_0000_rrrr_ddN0_cccc,		 "cpsdb   @%rw6,@%rw2,rr,%c7");
    public static Z8000_init op460 = new Z8000_init(0xba1c,0xbafc,16,2, 11,ZBA_ssN0_1100_0000_rrrr_dddd_cccc,		 "cpdrb   %rb6,@%rw2,rr,%c7");
    public static Z8000_init op461 = new Z8000_init(0xba1e,0xbafe,16,2, 11,ZBA_ssN0_1110_0000_rrrr_ddN0_cccc,		 "cpsdrb  @%rw6,@%rw2,rr,%c7");
    public static Z8000_init op462 = new Z8000_init(0xbb10,0xbbf0,16,2, 11,ZBB_ssN0_0000_0000_rrrr_dddd_cccc,		 "cpi     %rw6,@%rw2,rr,%c7");
    public static Z8000_init op463 = new Z8000_init(0xbb11,0xbbf1,16,2, 11,ZBB_ssN0_0001_0000_rrrr_ddN0_x000,		 "ldir    @%rw6,@%rw2,rr");
    public static Z8000_init op464 = new Z8000_init(0xbb12,0xbbf2,16,2, 11,ZBB_ssN0_0010_0000_rrrr_ddN0_cccc,		 "cpsi    @%rw6,@%rw2,rr,%c7");
    public static Z8000_init op465 = new Z8000_init(0xbb14,0xbbf4,16,2, 11,ZBB_ssN0_0100_0000_rrrr_dddd_cccc,		 "cpir    %rw6,@%rw2,rr,%c7");
    public static Z8000_init op466 = new Z8000_init(0xbb16,0xbbf6,16,2, 11,ZBB_ssN0_0110_0000_rrrr_ddN0_cccc,		 "cpsir   @%rw6,@%rw2,rr,%c7");
    public static Z8000_init op467 = new Z8000_init(0xbb18,0xbbf8,16,2, 11,ZBB_ssN0_1000_0000_rrrr_dddd_cccc,		 "cpd     %rw6,@%rw2,rr,%c7");
    public static Z8000_init op468 = new Z8000_init(0xbb19,0xbbf9,16,2, 11,ZBB_ssN0_1001_0000_rrrr_ddN0_x000,		 "lddr    @%rw2,@%rw6,rr");
    public static Z8000_init op469 = new Z8000_init(0xbb1a,0xbbfa,16,2, 11,ZBB_ssN0_1010_0000_rrrr_ddN0_cccc,		 "cpsd    @%rw6,@%rw2,rr,%c7");
    public static Z8000_init op470 = new Z8000_init(0xbb1c,0xbbfc,16,2, 11,ZBB_ssN0_1100_0000_rrrr_dddd_cccc,		 "cpdr    %rw6,@%rw2,rr,%c7");
    public static Z8000_init op471 = new Z8000_init(0xbb1e,0xbbfe,16,2, 11,ZBB_ssN0_1110_0000_rrrr_ddN0_cccc,		 "cpsdr   @%rw6,@%rw2,rr,%c7");
    public static Z8000_init op472 = new Z8000_init(0xbc00,0xbcff, 1,1,  9,ZBC_aaaa_bbbb,							 "rrdb    %rb3,%rb2");
    public static Z8000_init op473 = new Z8000_init(0xbd00,0xbdff, 1,1,  5,ZBD_dddd_imm4,							 "ldk     %rw2,%3");
    public static Z8000_init op474 = new Z8000_init(0xbe00,0xbeff, 1,1,  9,ZBE_aaaa_bbbb,							 "rldb    %rb3,%rb2");
    public static Z8000_init op475 = new Z8000_init(0xbf00,0xbfff, 1,1, 10,ZBF_imm8,								 "rsvdbf");
    public static Z8000_init op476 = new Z8000_init(0xc000,0xcfff, 1,1,  5,ZC_dddd_imm8,							 "ldb     %rb1,%#b1");
    public static Z8000_init op477 = new Z8000_init(0xd000,0xdfff, 1,1, 10,ZD_dsp12,								 "calr    %d2");
    public static Z8000_init op478 = new Z8000_init(0xe000,0xefff, 1,1,  6,ZE_cccc_dsp8,							 "jr      %c1,%d1");
    public static Z8000_init op479 = new Z8000_init(0xf000,0xf07f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op480 = new Z8000_init(0xf100,0xf17f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op481 = new Z8000_init(0xf200,0xf27f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op482 = new Z8000_init(0xf300,0xf37f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op483 = new Z8000_init(0xf400,0xf47f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op484 = new Z8000_init(0xf500,0xf57f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op485 = new Z8000_init(0xf600,0xf67f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op486 = new Z8000_init(0xf700,0xf77f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op487 = new Z8000_init(0xf800,0xf87f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op488 = new Z8000_init(0xf900,0xf97f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op489 = new Z8000_init(0xfa00,0xfa7f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op490 = new Z8000_init(0xfb00,0xfb7f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op491 = new Z8000_init(0xfc00,0xfc7f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op492 = new Z8000_init(0xfd00,0xfd7f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op493 = new Z8000_init(0xfe00,0xfe7f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op494 = new Z8000_init(0xff00,0xff7f, 1,1, 11,ZF_dddd_0dsp7,							 "dbjnz   %rb1,%d0");
    public static Z8000_init op495 = new Z8000_init(0xf080,0xf0ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op496 = new Z8000_init(0xf180,0xf1ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op497 = new Z8000_init(0xf280,0xf2ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op498 = new Z8000_init(0xf380,0xf3ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op499 = new Z8000_init(0xf480,0xf4ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op500 = new Z8000_init(0xf580,0xf5ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op501 = new Z8000_init(0xf680,0xf6ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op502 = new Z8000_init(0xf780,0xf7ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op503 = new Z8000_init(0xf880,0xf8ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op504 = new Z8000_init(0xf980,0xf9ff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op505 = new Z8000_init(0xfa80,0xfaff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op506 = new Z8000_init(0xfb80,0xfbff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op507 = new Z8000_init(0xfc80,0xfcff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op508 = new Z8000_init(0xfd80,0xfdff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op509 = new Z8000_init(0xfe80,0xfeff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op510 = new Z8000_init(0xff80,0xffff, 1,1, 11,ZF_dddd_1dsp7,							 "djnz    %rw1,%d0");
    public static Z8000_init op511 = new Z8000_init(0, 	0,	   0,	0,	0,null,									 null);
}
