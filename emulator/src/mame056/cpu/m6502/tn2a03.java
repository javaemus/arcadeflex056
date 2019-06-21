/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502.*;
import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.opsn2a03H.*;

public class tn2a03 {

    /**
     * ***************************************************************************
     *****************************************************************************
     *
     * overrides for 2a03 opcodes
     *
     *****************************************************************************
     ********** insn temp cycles	rdmem opc	wrmem	*********
     */
    static opcode n2a03_00 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            BRK();
        }
    };
    /* 7 BRK */
    static opcode n2a03_20 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            JSR();
        }
    };
    /* 6 JSR */
    static opcode n2a03_40 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            RTI();
        }
    };
    /* 6 RTI */
    static opcode n2a03_60 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            RTS();
        }
    };
    /* 6 RTS */
    static opcode n2a03_80 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_a0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDY(tmp);
        }
    };
    /* 2 LDY IMM */
    static opcode n2a03_c0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CPY(tmp);
        }
    };
    /* 2 CPY IMM */
    static opcode n2a03_e0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CPX(tmp);
        }
    };
    /* 2 CPX IMM */

    static opcode n2a03_10 = new opcode() {
        public void handler() {
            BPL();
        }
    };
    /* 2 BPL REL */
    static opcode n2a03_30 = new opcode() {
        public void handler() {
            BMI();
        }
    };
    /* 2 BMI REL */
    static opcode n2a03_50 = new opcode() {
        public void handler() {
            BVC();
        }
    };
    /* 2 BVC REL */
    static opcode n2a03_70 = new opcode() {
        public void handler() {
            BVS();
        }
    };
    /* 2 BVS REL */
    static opcode n2a03_90 = new opcode() {
        public void handler() {
            BCC();
        }
    };
    /* 2 BCC REL */
    static opcode n2a03_b0 = new opcode() {
        public void handler() {
            BCS();
        }
    };
    /* 2 BCS REL */
    static opcode n2a03_d0 = new opcode() {
        public void handler() {
            BNE();
        }
    };
    /* 2 BNE REL */
    static opcode n2a03_f0 = new opcode() {
        public void handler() {
            BEQ();
        }
    };
    /* 2 BEQ REL */

    static opcode n2a03_01 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            ORA(tmp);
        }
    };
    /* 6 ORA IDX */
    static opcode n2a03_21 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            AND(tmp);
        }
    };
    /* 6 AND IDX */
    static opcode n2a03_41 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            EOR(tmp);
        }
    };
    /* 6 EOR IDX */
    static opcode n2a03_61 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            ADC_NES(tmp);
        }
    };
    /* 6 ADC IDX */
    static opcode n2a03_81 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = STA();
            WR_IDX(tmp);
        }
    };
    /* 6 STA IDX */
    static opcode n2a03_a1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            LDA(tmp);
        }
    };
    /* 6 LDA IDX */
    static opcode n2a03_c1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            CMP(tmp);
        }
    };
    /* 6 CMP IDX */
    static opcode n2a03_e1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            SBC_NES(tmp);
        }
    };
    /* 6 SBC IDX */

    static opcode n2a03_11 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            ORA(tmp);
        }
    };
    /* 5 ORA IDY */
    static opcode n2a03_31 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            AND(tmp);
        }
    };
    /* 5 AND IDY */
    static opcode n2a03_51 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            EOR(tmp);
        }
    };
    /* 5 EOR IDY */
    static opcode n2a03_71 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            ADC_NES(tmp);
        }
    };
    /* 5 ADC IDY */
    static opcode n2a03_91 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = STA();
            WR_IDY(tmp);
        }
    };
    /* 6 STA IDY */
    static opcode n2a03_b1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            LDA(tmp);
        }
    };
    /* 5 LDA IDY */
    static opcode n2a03_d1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            CMP(tmp);
        }
    };
    /* 5 CMP IDY */
    static opcode n2a03_f1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            SBC_NES(tmp);
        }
    };
    /* 5 SBC IDY */

    static opcode n2a03_02 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_22 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_42 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_62 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_82 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_a2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDX(tmp);
        }
    };
    /* 2 LDX IMM */
    static opcode n2a03_c2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_e2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_12 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_32 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_52 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_72 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_92 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_b2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_d2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_f2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_03 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_23 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_43 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_63 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_83 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_a3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_c3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_e3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_13 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_33 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_53 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_73 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_93 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_b3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_d3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_f3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_04 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_24 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            BIT(tmp);
        }
    };
    /* 3 BIT ZPG */
    static opcode n2a03_44 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_64 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_84 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STY();
            WR_ZPG(tmp);
        }
    };
    /* 3 STY ZPG */
    static opcode n2a03_a4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDY(tmp);
        }
    };
    /* 3 LDY ZPG */
    static opcode n2a03_c4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CPY(tmp);
        }
    };
    /* 3 CPY ZPG */
    static opcode n2a03_e4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CPX(tmp);
        }
    };
    /* 3 CPX ZPG */

    static opcode n2a03_14 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_34 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_54 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_74 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_94 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STY();
            WR_ZPX(tmp);
        }
    };
    /* 4 STY ZPX */
    static opcode n2a03_b4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            LDY(tmp);
        }
    };
    /* 4 LDY ZPX */
    static opcode n2a03_d4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_f4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_05 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            ORA(tmp);
        }
    };
    /* 3 ORA ZPG */
    static opcode n2a03_25 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            AND(tmp);
        }
    };
    /* 3 AND ZPG */
    static opcode n2a03_45 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            EOR(tmp);
        }
    };
    /* 3 EOR ZPG */
    static opcode n2a03_65 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            ADC_NES(tmp);
        }
    };
    /* 3 ADC ZPG */
    static opcode n2a03_85 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STA();
            WR_ZPG(tmp);
        }
    };
    /* 3 STA ZPG */
    static opcode n2a03_a5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDA(tmp);
        }
    };
    /* 3 LDA ZPG */
    static opcode n2a03_c5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CMP(tmp);
        }
    };
    /* 3 CMP ZPG */
    static opcode n2a03_e5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            SBC_NES(tmp);
        }
    };
    /* 3 SBC ZPG */

    static opcode n2a03_15 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            ORA(tmp);
        }
    };
    /* 4 ORA ZPX */
    static opcode n2a03_35 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            AND(tmp);
        }
    };
    /* 4 AND ZPX */
    static opcode n2a03_55 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            EOR(tmp);
        }
    };
    /* 4 EOR ZPX */
    static opcode n2a03_75 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            ADC_NES(tmp);
        }
    };
    /* 4 ADC ZPX */
    static opcode n2a03_95 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STA();
            WR_ZPX(tmp);
        }
    };
    /* 4 STA ZPX */
    static opcode n2a03_b5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            LDA(tmp);
        }
    };
    /* 4 LDA ZPX */
    static opcode n2a03_d5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            CMP(tmp);
        }
    };
    /* 4 CMP ZPX */
    static opcode n2a03_f5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            SBC_NES(tmp);
        }
    };
    /* 4 SBC ZPX */

    static opcode n2a03_06 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 ASL ZPG */
    static opcode n2a03_26 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 ROL ZPG */
    static opcode n2a03_46 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 int tmp2=LSR(tmp); ZPG */
    static opcode n2a03_66 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 int tmp2=ROR(tmp); ZPG */
    static opcode n2a03_86 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STX();
            WR_ZPG(tmp);
        }
    };
    /* 3 STX ZPG */
    static opcode n2a03_a6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDX(tmp);
        }
    };
    /* 3 LDX ZPG */
    static opcode n2a03_c6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 DEC ZPG */
    static opcode n2a03_e6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 INC ZPG */

    static opcode n2a03_16 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 ASL ZPX */
    static opcode n2a03_36 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 ROL ZPX */
    static opcode n2a03_56 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 int tmp2=LSR(tmp); ZPX */
    static opcode n2a03_76 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 int tmp2=ROR(tmp); ZPX */
    static opcode n2a03_96 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STX();
            WR_ZPY(tmp);
        }
    };
    /* 4 STX ZPY */
    static opcode n2a03_b6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPY();
            LDX(tmp);
        }
    };
    /* 4 LDX ZPY */
    static opcode n2a03_d6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 DEC ZPX */
    static opcode n2a03_f6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 INC ZPX */

    static opcode n2a03_07 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_27 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_47 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_67 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_87 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_a7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_c7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_e7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_17 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_37 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_57 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_77 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_97 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_b7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_d7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_f7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_08 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PHP();
        }
    };
    /* 2 PHP */
    static opcode n2a03_28 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PLP();
        }
    };
    /* 2 PLP */
    static opcode n2a03_48 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PHA();
        }
    };
    /* 2 PHA */
    static opcode n2a03_68 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PLA();
        }
    };
    /* 2 PLA */
    static opcode n2a03_88 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEY();
        }
    };
    /* 2 DEY */
    static opcode n2a03_a8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TAY();
        }
    };
    /* 2 TAY */
    static opcode n2a03_c8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INY();
        }
    };
    /* 2 INY */
    static opcode n2a03_e8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INX();
        }
    };
    /* 2 INX */

    static opcode n2a03_18 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLC();
        }
    };
    /* 2 CLC */
    static opcode n2a03_38 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SEC();
        }
    };
    /* 2 SEC */
    static opcode n2a03_58 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLI();
        }
    };
    /* 2 CLI */
    static opcode n2a03_78 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SEI();
        }
    };
    /* 2 SEI */
    static opcode n2a03_98 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TYA();
        }
    };
    /* 2 TYA */
    static opcode n2a03_b8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLV();
        }
    };
    /* 2 CLV */
    static opcode n2a03_d8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLD();
        }
    };
    /* 2 CLD */
    static opcode n2a03_f8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SED();
        }
    };
    /* 2 SED */

    static opcode n2a03_09 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            ORA(tmp);
        }
    };
    /* 2 ORA IMM */
    static opcode n2a03_29 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            AND(tmp);
        }
    };
    /* 2 AND IMM */
    static opcode n2a03_49 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            EOR(tmp);
        }
    };
    /* 2 EOR IMM */
    static opcode n2a03_69 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            ADC_NES(tmp);
        }
    };
    /* 2 ADC IMM */
    static opcode n2a03_89 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_a9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDA(tmp);
        }
    };
    /* 2 LDA IMM */
    static opcode n2a03_c9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CMP(tmp);
        }
    };
    /* 2 CMP IMM */
    static opcode n2a03_e9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            SBC_NES(tmp);
        }
    };
    /* 2 SBC IMM */

    static opcode n2a03_19 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            ORA(tmp);
        }
    };
    /* 4 ORA ABY */
    static opcode n2a03_39 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            AND(tmp);
        }
    };
    /* 4 AND ABY */
    static opcode n2a03_59 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            EOR(tmp);
        }
    };
    /* 4 EOR ABY */
    static opcode n2a03_79 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            ADC_NES(tmp);
        }
    };
    /* 4 ADC ABY */
    static opcode n2a03_99 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STA();
            WR_ABY(tmp);
        }
    };
    /* 5 STA ABY */
    static opcode n2a03_b9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            LDA(tmp);
        }
    };
    /* 4 LDA ABY */
    static opcode n2a03_d9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            CMP(tmp);
        }
    };
    /* 4 CMP ABY */
    static opcode n2a03_f9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            SBC_NES(tmp);
        }
    };
    /* 4 SBC ABY */

    static opcode n2a03_0a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ASL(tmp);
            WB_ACC(tmp2);
        }
    };
    /* 2 ASL A */
    static opcode n2a03_2a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ROL(tmp);
            WB_ACC(tmp2);
        }
    };
    /* 2 ROL A */
    static opcode n2a03_4a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = LSR(tmp);
            WB_ACC(tmp2);
        }
    };
    /* 2 int tmp2=LSR(tmp); A */
    static opcode n2a03_6a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ROR(tmp);
            WB_ACC(tmp2);
        }
    };
    /* 2 int tmp2=ROR(tmp); A */
    static opcode n2a03_8a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TXA();
        }
    };
    /* 2 TXA */
    static opcode n2a03_aa = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TAX();
        }
    };
    /* 2 TAX */
    static opcode n2a03_ca = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEX();
        }
    };
    /* 2 DEX */
    static opcode n2a03_ea = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            NOP();
        }
    };
    /* 2 NOP */

    static opcode n2a03_1a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_3a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_5a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_7a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_9a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TXS();
        }
    };
    /* 2 TXS */
    static opcode n2a03_ba = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TSX();
        }
    };
    /* 2 TSX */
    static opcode n2a03_da = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_fa = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_0b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_2b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_4b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_6b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_8b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_ab = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_cb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_eb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_1b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_3b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_5b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_7b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_9b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_bb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_db = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_fb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_0c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_2c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            BIT(tmp);
        }
    };
    /* 4 BIT ABS */
    static opcode n2a03_4c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            EA_ABS();
            JMP();
        }
    };
    /* 3 JMP ABS */
    static opcode n2a03_6c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            EA_IND();
            JMP();
        }
    };
    /* 5 JMP IND */
    static opcode n2a03_8c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STY();
            WR_ABS(tmp);
        }
    };
    /* 4 STY ABS */
    static opcode n2a03_ac = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDY(tmp);
        }
    };
    /* 4 LDY ABS */
    static opcode n2a03_cc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CPY(tmp);
        }
    };
    /* 4 CPY ABS */
    static opcode n2a03_ec = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CPX(tmp);
        }
    };
    /* 4 CPX ABS */

    static opcode n2a03_1c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_3c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_5c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_7c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_9c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_bc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            LDY(tmp);
        }
    };
    /* 4 LDY ABX */
    static opcode n2a03_dc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_fc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_0d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            ORA(tmp);
        }
    };
    /* 4 ORA ABS */
    static opcode n2a03_2d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            AND(tmp);
        }
    };
    /* 4 AND ABS */
    static opcode n2a03_4d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            EOR(tmp);
        }
    };
    /* 4 EOR ABS */
    static opcode n2a03_6d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            ADC_NES(tmp);
        }
    };
    /* 4 ADC ABS */
    static opcode n2a03_8d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STA();
            WR_ABS(tmp);
        }
    };
    /* 4 STA ABS */
    static opcode n2a03_ad = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDA(tmp);
        }
    };
    /* 4 LDA ABS */
    static opcode n2a03_cd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CMP(tmp);
        }
    };
    /* 4 CMP ABS */
    static opcode n2a03_ed = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            SBC_NES(tmp);
        }
    };
    /* 4 SBC ABS */

    static opcode n2a03_1d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            ORA(tmp);
        }
    };
    /* 4 ORA ABX */
    static opcode n2a03_3d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            AND(tmp);
        }
    };
    /* 4 AND ABX */
    static opcode n2a03_5d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            EOR(tmp);
        }
    };
    /* 4 EOR ABX */
    static opcode n2a03_7d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            ADC_NES(tmp);
        }
    };
    /* 4 ADC ABX */
    static opcode n2a03_9d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STA();
            WR_ABX(tmp);
        }
    };
    /* 5 STA ABX */
    static opcode n2a03_bd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            LDA(tmp);
        }
    };
    /* 4 LDA ABX */
    static opcode n2a03_dd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            CMP(tmp);
        }
    };
    /* 4 CMP ABX */
    static opcode n2a03_fd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            SBC_NES(tmp);
        }
    };
    /* 4 SBC ABX */

    static opcode n2a03_0e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 ASL ABS */
    static opcode n2a03_2e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 ROL ABS */
    static opcode n2a03_4e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 int tmp2=LSR(tmp); ABS */
    static opcode n2a03_6e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 int tmp2=ROR(tmp); ABS */
    static opcode n2a03_8e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STX();
            WR_ABS(tmp);
        }
    };
    /* 5 STX ABS */
    static opcode n2a03_ae = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDX(tmp);
        }
    };
    /* 4 LDX ABS */
    static opcode n2a03_ce = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 DEC ABS */
    static opcode n2a03_ee = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 INC ABS */

    static opcode n2a03_1e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 ASL ABX */
    static opcode n2a03_3e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 ROL ABX */
    static opcode n2a03_5e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 int tmp2=LSR(tmp); ABX */
    static opcode n2a03_7e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 int tmp2=ROR(tmp); ABX */
    static opcode n2a03_9e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_be = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            LDX(tmp);
        }
    };
    /* 4 LDX ABY */
    static opcode n2a03_de = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 DEC ABX */
    static opcode n2a03_fe = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 INC ABX */

    static opcode n2a03_0f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_2f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_4f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_6f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_8f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_af = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_cf = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_ef = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode n2a03_1f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_3f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_5f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_7f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_9f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_bf = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_df = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode n2a03_ff = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

 /* and here's the array of function pointers */
    public static opcode[] insn2a03 = {
        n2a03_00, n2a03_01, n2a03_02, n2a03_03, n2a03_04, n2a03_05, n2a03_06, n2a03_07,
        n2a03_08, n2a03_09, n2a03_0a, n2a03_0b, n2a03_0c, n2a03_0d, n2a03_0e, n2a03_0f,
        n2a03_10, n2a03_11, n2a03_12, n2a03_13, n2a03_14, n2a03_15, n2a03_16, n2a03_17,
        n2a03_18, n2a03_19, n2a03_1a, n2a03_1b, n2a03_1c, n2a03_1d, n2a03_1e, n2a03_1f,
        n2a03_20, n2a03_21, n2a03_22, n2a03_23, n2a03_24, n2a03_25, n2a03_26, n2a03_27,
        n2a03_28, n2a03_29, n2a03_2a, n2a03_2b, n2a03_2c, n2a03_2d, n2a03_2e, n2a03_2f,
        n2a03_30, n2a03_31, n2a03_32, n2a03_33, n2a03_34, n2a03_35, n2a03_36, n2a03_37,
        n2a03_38, n2a03_39, n2a03_3a, n2a03_3b, n2a03_3c, n2a03_3d, n2a03_3e, n2a03_3f,
        n2a03_40, n2a03_41, n2a03_42, n2a03_43, n2a03_44, n2a03_45, n2a03_46, n2a03_47,
        n2a03_48, n2a03_49, n2a03_4a, n2a03_4b, n2a03_4c, n2a03_4d, n2a03_4e, n2a03_4f,
        n2a03_50, n2a03_51, n2a03_52, n2a03_53, n2a03_54, n2a03_55, n2a03_56, n2a03_57,
        n2a03_58, n2a03_59, n2a03_5a, n2a03_5b, n2a03_5c, n2a03_5d, n2a03_5e, n2a03_5f,
        n2a03_60, n2a03_61, n2a03_62, n2a03_63, n2a03_64, n2a03_65, n2a03_66, n2a03_67,
        n2a03_68, n2a03_69, n2a03_6a, n2a03_6b, n2a03_6c, n2a03_6d, n2a03_6e, n2a03_6f,
        n2a03_70, n2a03_71, n2a03_72, n2a03_73, n2a03_74, n2a03_75, n2a03_76, n2a03_77,
        n2a03_78, n2a03_79, n2a03_7a, n2a03_7b, n2a03_7c, n2a03_7d, n2a03_7e, n2a03_7f,
        n2a03_80, n2a03_81, n2a03_82, n2a03_83, n2a03_84, n2a03_85, n2a03_86, n2a03_87,
        n2a03_88, n2a03_89, n2a03_8a, n2a03_8b, n2a03_8c, n2a03_8d, n2a03_8e, n2a03_8f,
        n2a03_90, n2a03_91, n2a03_92, n2a03_93, n2a03_94, n2a03_95, n2a03_96, n2a03_97,
        n2a03_98, n2a03_99, n2a03_9a, n2a03_9b, n2a03_9c, n2a03_9d, n2a03_9e, n2a03_9f,
        n2a03_a0, n2a03_a1, n2a03_a2, n2a03_a3, n2a03_a4, n2a03_a5, n2a03_a6, n2a03_a7,
        n2a03_a8, n2a03_a9, n2a03_aa, n2a03_ab, n2a03_ac, n2a03_ad, n2a03_ae, n2a03_af,
        n2a03_b0, n2a03_b1, n2a03_b2, n2a03_b3, n2a03_b4, n2a03_b5, n2a03_b6, n2a03_b7,
        n2a03_b8, n2a03_b9, n2a03_ba, n2a03_bb, n2a03_bc, n2a03_bd, n2a03_be, n2a03_bf,
        n2a03_c0, n2a03_c1, n2a03_c2, n2a03_c3, n2a03_c4, n2a03_c5, n2a03_c6, n2a03_c7,
        n2a03_c8, n2a03_c9, n2a03_ca, n2a03_cb, n2a03_cc, n2a03_cd, n2a03_ce, n2a03_cf,
        n2a03_d0, n2a03_d1, n2a03_d2, n2a03_d3, n2a03_d4, n2a03_d5, n2a03_d6, n2a03_d7,
        n2a03_d8, n2a03_d9, n2a03_da, n2a03_db, n2a03_dc, n2a03_dd, n2a03_de, n2a03_df,
        n2a03_e0, n2a03_e1, n2a03_e2, n2a03_e3, n2a03_e4, n2a03_e5, n2a03_e6, n2a03_e7,
        n2a03_e8, n2a03_e9, n2a03_ea, n2a03_eb, n2a03_ec, n2a03_ed, n2a03_ee, n2a03_ef,
        n2a03_f0, n2a03_f1, n2a03_f2, n2a03_f3, n2a03_f4, n2a03_f5, n2a03_f6, n2a03_f7,
        n2a03_f8, n2a03_f9, n2a03_fa, n2a03_fb, n2a03_fc, n2a03_fd, n2a03_fe, n2a03_ff
    };

    public abstract interface opcode {

        public abstract void handler();
    }

}
