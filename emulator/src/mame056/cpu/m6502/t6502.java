/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.m6502.*;
import static mame056.cpu.m6502.ops02H.*;

public class t6502 {

    /**
     * ***************************************************************************
     *****************************************************************************
     *
     * plain vanilla 6502 opcodes
     *
     *****************************************************************************
     * op	temp	cycles	rdmem	opc wrmem *******************
     */
    static opcode m6502_00 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            BRK();
        }
    };
    /* 7 BRK */
    static opcode m6502_20 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            JSR();
        }
    };
    /* 6 JSR */
    static opcode m6502_40 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            RTI();
        }
    };
    /* 6 RTI */
    static opcode m6502_60 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            RTS();
        }
    };
    /* 6 RTS */
    static opcode m6502_80 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_a0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDY(tmp);
        }
    };
    /* 2 LDY IMM */
    static opcode m6502_c0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CPY(tmp);
        }
    };
    /* 2 CPY IMM */
    static opcode m6502_e0 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CPX(tmp);
        }
    };
    /* 2 CPX IMM */

    static opcode m6502_10 = new opcode() {
        public void handler() {
            BPL();
        }
    };
    /* 2 BPL REL */
    static opcode m6502_30 = new opcode() {
        public void handler() {
            BMI();
        }
    };
    /* 2 BMI REL */
    static opcode m6502_50 = new opcode() {
        public void handler() {
            BVC();
        }
    };
    /* 2 BVC REL */
    static opcode m6502_70 = new opcode() {
        public void handler() {
            BVS();
        }
    };
    /* 2 BVS REL */
    static opcode m6502_90 = new opcode() {
        public void handler() {
            BCC();
        }
    };
    /* 2 BCC REL */
    static opcode m6502_b0 = new opcode() {
        public void handler() {
            BCS();
        }
    };
    /* 2 BCS REL */
    static opcode m6502_d0 = new opcode() {
        public void handler() {
            BNE();
        }
    };
    /* 2 BNE REL */
    static opcode m6502_f0 = new opcode() {
        public void handler() {
            BEQ();
        }
    };
    /* 2 BEQ REL */

    static opcode m6502_01 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            ORA(tmp);
        }
    };
    /* 6 ORA IDX */
    static opcode m6502_21 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            AND(tmp);
        }
    };
    /* 6 AND IDX */
    static opcode m6502_41 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            EOR(tmp);
        }
    };
    /* 6 EOR IDX */
    static opcode m6502_61 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            ADC(tmp);
        }
    };
    /* 6 ADC IDX */
    static opcode m6502_81 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = STA();
            WR_IDX(tmp);
        }
    };
    /* 6 STA IDX */
    static opcode m6502_a1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            LDA(tmp);
        }
    };
    /* 6 LDA IDX */
    static opcode m6502_c1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            CMP(tmp);
        }
    };
    /* 6 CMP IDX */
    static opcode m6502_e1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_IDX();
            SBC(tmp);
        }
    };
    /* 6 SBC IDX */

    static opcode m6502_11 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            ORA(tmp);
        }
    };
    /* 5 ORA IDY */
    static opcode m6502_31 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            AND(tmp);
        }
    };
    /* 5 AND IDY */
    static opcode m6502_51 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            EOR(tmp);
        }
    };
    /* 5 EOR IDY */
    static opcode m6502_71 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            ADC(tmp);
        }
    };
    /* 5 ADC IDY */
    static opcode m6502_91 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = STA();
            WR_IDY(tmp);
        }
    };
    /* 6 STA IDY */
    static opcode m6502_b1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            LDA(tmp);
        }
    };
    /* 5 LDA IDY */
    static opcode m6502_d1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            CMP(tmp);
        }
    };
    /* 5 CMP IDY */
    static opcode m6502_f1 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_IDY();
            SBC(tmp);
        }
    };
    /* 5 SBC IDY */

    static opcode m6502_02 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_22 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_42 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_62 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_82 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_a2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDX(tmp);
        }
    };
    /* 2 LDX IMM */
    static opcode m6502_c2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_e2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_12 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_32 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_52 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_72 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_92 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_b2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_d2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_f2 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_03 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_23 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_43 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_63 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_83 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_a3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_c3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_e3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_13 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_33 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_53 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_73 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_93 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_b3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_d3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_f3 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_04 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_24 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            BIT(tmp);
        }
    };
    /* 3 BIT ZPG */
    static opcode m6502_44 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_64 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_84 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STY();
            WR_ZPG(tmp);
        }
    };
    /* 3 STY ZPG */
    static opcode m6502_a4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDY(tmp);
        }
    };
    /* 3 LDY ZPG */
    static opcode m6502_c4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CPY(tmp);
        }
    };
    /* 3 CPY ZPG */
    static opcode m6502_e4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CPX(tmp);
        }
    };
    /* 3 CPX ZPG */

    static opcode m6502_14 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_34 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_54 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_74 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_94 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STY();
            WR_ZPX(tmp);
        }
    };
    /* 4 STY ZPX */
    static opcode m6502_b4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            LDY(tmp);
        }
    };
    /* 4 LDY ZPX */
    static opcode m6502_d4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_f4 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_05 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            ORA(tmp);
        }
    };
    /* 3 ORA ZPG */
    static opcode m6502_25 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            AND(tmp);
        }
    };
    /* 3 AND ZPG */
    static opcode m6502_45 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            EOR(tmp);
        }
    };
    /* 3 EOR ZPG */
    static opcode m6502_65 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            ADC(tmp);
        }
    };
    /* 3 ADC ZPG */
    static opcode m6502_85 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STA();
            WR_ZPG(tmp);
        }
    };
    /* 3 STA ZPG */
    static opcode m6502_a5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDA(tmp);
        }
    };
    /* 3 LDA ZPG */
    static opcode m6502_c5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            CMP(tmp);
        }
    };
    /* 3 CMP ZPG */
    static opcode m6502_e5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            SBC(tmp);
        }
    };
    /* 3 SBC ZPG */

    static opcode m6502_15 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            ORA(tmp);
        }
    };
    /* 4 ORA ZPX */
    static opcode m6502_35 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            AND(tmp);
        }
    };
    /* 4 AND ZPX */
    static opcode m6502_55 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            EOR(tmp);
        }
    };
    /* 4 EOR ZPX */
    static opcode m6502_75 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            ADC(tmp);
        }
    };
    /* 4 ADC ZPX */
    static opcode m6502_95 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STA();
            WR_ZPX(tmp);
        }
    };
    /* 4 STA ZPX */
    static opcode m6502_b5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            LDA(tmp);
        }
    };
    /* 4 LDA ZPX */
    static opcode m6502_d5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            CMP(tmp);
        }
    };
    /* 4 CMP ZPX */
    static opcode m6502_f5 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPX();
            SBC(tmp);
        }
    };
    /* 4 SBC ZPX */

    static opcode m6502_06 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 ASL ZPG */
    static opcode m6502_26 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 ROL ZPG */
    static opcode m6502_46 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 int tmp2=LSR(tmp); ZPG */
    static opcode m6502_66 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 int tmp2=ROR(tmp); ZPG */
    static opcode m6502_86 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = STX();
            WR_ZPG(tmp);
        }
    };
    /* 3 STX ZPG */
    static opcode m6502_a6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            int tmp = RD_ZPG();
            LDX(tmp);
        }
    };
    /* 3 LDX ZPG */
    static opcode m6502_c6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 DEC ZPG */
    static opcode m6502_e6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = RD_ZPG();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 5 INC ZPG */

    static opcode m6502_16 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 ASL ZPX */
    static opcode m6502_36 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 ROL ZPX */
    static opcode m6502_56 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 int tmp2=LSR(tmp); ZPX */
    static opcode m6502_76 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 int tmp2=ROR(tmp); ZPX */
    static opcode m6502_96 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STX();
            WR_ZPY(tmp);
        }
    };
    /* 4 STX ZPY */
    static opcode m6502_b6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ZPY();
            LDX(tmp);
        }
    };
    /* 4 LDX ZPY */
    static opcode m6502_d6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 DEC ZPX */
    static opcode m6502_f6 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ZPX();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 INC ZPX */

    static opcode m6502_07 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_27 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_47 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_67 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_87 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_a7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_c7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_e7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_17 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_37 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_57 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_77 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_97 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_b7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_d7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_f7 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_08 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PHP();
        }
    };
    /* 2 PHP */
    static opcode m6502_28 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PLP();
        }
    };
    /* 2 PLP */
    static opcode m6502_48 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PHA();
        }
    };
    /* 2 PHA */
    static opcode m6502_68 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            PLA();
        }
    };
    /* 2 PLA */
    static opcode m6502_88 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEY();
        }
    };
    /* 2 DEY */
    static opcode m6502_a8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TAY();
        }
    };
    /* 2 TAY */
    static opcode m6502_c8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INY();
        }
    };
    /* 2 INY */
    static opcode m6502_e8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            INX();
        }
    };
    /* 2 INX */

    static opcode m6502_18 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLC();
        }
    };
    /* 2 CLC */
    static opcode m6502_38 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SEC();
        }
    };
    /* 2 SEC */
    static opcode m6502_58 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLI();
        }
    };
    /* 2 CLI */
    static opcode m6502_78 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SEI();
        }
    };
    /* 2 SEI */
    static opcode m6502_98 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TYA();
        }
    };
    /* 2 TYA */
    static opcode m6502_b8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLV();
        }
    };
    /* 2 CLV */
    static opcode m6502_d8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            CLD();
        }
    };
    /* 2 CLD */
    static opcode m6502_f8 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            SED();
        }
    };
    /* 2 SED */

    static opcode m6502_09 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            ORA(tmp);
        }
    };
    /* 2 ORA IMM */
    static opcode m6502_29 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            AND(tmp);
        }
    };
    /* 2 AND IMM */
    static opcode m6502_49 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            EOR(tmp);
        }
    };
    /* 2 EOR IMM */
    static opcode m6502_69 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            ADC(tmp);
        }
    };
    /* 2 ADC IMM */
    static opcode m6502_89 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_a9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            LDA(tmp);
        }
    };
    /* 2 LDA IMM */
    static opcode m6502_c9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            CMP(tmp);
        }
    };
    /* 2 CMP IMM */
    static opcode m6502_e9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_IMM();
            SBC(tmp);
        }
    };
    /* 2 SBC IMM */

    static opcode m6502_19 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            ORA(tmp);
        }
    };
    /* 4 ORA ABY */
    static opcode m6502_39 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            AND(tmp);
        }
    };
    /* 4 AND ABY */
    static opcode m6502_59 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            EOR(tmp);
        }
    };
    /* 4 EOR ABY */
    static opcode m6502_79 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            ADC(tmp);
        }
    };
    /* 4 ADC ABY */
    static opcode m6502_99 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STA();
            WR_ABY(tmp);
        }
    };
    /* 5 STA ABY */
    static opcode m6502_b9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            LDA(tmp);
        }
    };
    /* 4 LDA ABY */
    static opcode m6502_d9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            CMP(tmp);
        }
    };
    /* 4 CMP ABY */
    static opcode m6502_f9 = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            SBC(tmp);
        }
    };
    /* 4 SBC ABY */

    static opcode m6502_0a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ASL(tmp);
            WB_ACC(tmp2);
        }
    };
    /* 2 ASL A */
    static opcode m6502_2a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ROL(tmp);
            WB_ACC(tmp2);
        }
    };
    /* 2 ROL A */
    static opcode m6502_4a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = LSR(tmp);
            WB_ACC(tmp2);
        }
    };
    /* 2 int tmp2=LSR(tmp); A */
    static opcode m6502_6a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            int tmp = RD_ACC();
            int tmp2 = ROR(tmp);
            WB_ACC(tmp2);
        }
    };
    /* 2 int tmp2=ROR(tmp); A */
    static opcode m6502_8a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TXA();
        }
    };
    /* 2 TXA */
    static opcode m6502_aa = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TAX();
        }
    };
    /* 2 TAX */
    static opcode m6502_ca = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            DEX();
        }
    };
    /* 2 DEX */
    static opcode m6502_ea = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            NOP();
        }
    };
    /* 2 NOP */

    static opcode m6502_1a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_3a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_5a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_7a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_9a = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TXS();
        }
    };
    /* 2 TXS */
    static opcode m6502_ba = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            TSX();
        }
    };
    /* 2 TSX */
    static opcode m6502_da = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_fa = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_0b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_2b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_4b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_6b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_8b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_ab = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_cb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_eb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_1b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_3b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_5b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_7b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_9b = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_bb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_db = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_fb = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_0c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_2c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            BIT(tmp);
        }
    };
    /* 4 BIT ABS */
    static opcode m6502_4c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 3;
            EA_ABS();
            JMP();
        }
    };
    /* 3 JMP ABS */
    static opcode m6502_6c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            EA_IND();
            JMP();
        }
    };
    /* 5 JMP IND */
    static opcode m6502_8c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STY();
            WR_ABS(tmp);
        }
    };
    /* 4 STY ABS */
    static opcode m6502_ac = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDY(tmp);
        }
    };
    /* 4 LDY ABS */
    static opcode m6502_cc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CPY(tmp);
        }
    };
    /* 4 CPY ABS */
    static opcode m6502_ec = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CPX(tmp);
        }
    };
    /* 4 CPX ABS */

    static opcode m6502_1c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_3c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_5c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_7c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_9c = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_bc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            LDY(tmp);
        }
    };
    /* 4 LDY ABX */
    static opcode m6502_dc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_fc = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_0d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            ORA(tmp);
        }
    };
    /* 4 ORA ABS */
    static opcode m6502_2d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            AND(tmp);
        }
    };
    /* 4 AND ABS */
    static opcode m6502_4d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            EOR(tmp);
        }
    };
    /* 4 EOR ABS */
    static opcode m6502_6d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            ADC(tmp);
        }
    };
    /* 4 ADC ABS */
    static opcode m6502_8d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = STA();
            WR_ABS(tmp);
        }
    };
    /* 4 STA ABS */
    static opcode m6502_ad = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDA(tmp);
        }
    };
    /* 4 LDA ABS */
    static opcode m6502_cd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            CMP(tmp);
        }
    };
    /* 4 CMP ABS */
    static opcode m6502_ed = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            SBC(tmp);
        }
    };
    /* 4 SBC ABS */

    static opcode m6502_1d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            ORA(tmp);
        }
    };
    /* 4 ORA ABX */
    static opcode m6502_3d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            AND(tmp);
        }
    };
    /* 4 AND ABX */
    static opcode m6502_5d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            EOR(tmp);
        }
    };
    /* 4 EOR ABX */
    static opcode m6502_7d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            ADC(tmp);
        }
    };
    /* 4 ADC ABX */
    static opcode m6502_9d = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STA();
            WR_ABX(tmp);
        }
    };
    /* 5 STA ABX */
    static opcode m6502_bd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            LDA(tmp);
        }
    };
    /* 4 LDA ABX */
    static opcode m6502_dd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            CMP(tmp);
        }
    };
    /* 4 CMP ABX */
    static opcode m6502_fd = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABX();
            SBC(tmp);
        }
    };
    /* 4 SBC ABX */

    static opcode m6502_0e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 ASL ABS */
    static opcode m6502_2e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 ROL ABS */
    static opcode m6502_4e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 int tmp2=LSR(tmp); ABS */
    static opcode m6502_6e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 int tmp2=ROR(tmp); ABS */
    static opcode m6502_8e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 5;
            int tmp = STX();
            WR_ABS(tmp);
        }
    };
    /* 5 STX ABS */
    static opcode m6502_ae = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABS();
            LDX(tmp);
        }
    };
    /* 4 LDX ABS */
    static opcode m6502_ce = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 DEC ABS */
    static opcode m6502_ee = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 6;
            int tmp = RD_ABS();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 6 INC ABS */

    static opcode m6502_1e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ASL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 ASL ABX */
    static opcode m6502_3e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ROL(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 ROL ABX */
    static opcode m6502_5e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = LSR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 int tmp2=LSR(tmp); ABX */
    static opcode m6502_7e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = ROR(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 int tmp2=ROR(tmp); ABX */
    static opcode m6502_9e = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_be = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 4;
            int tmp = RD_ABY();
            LDX(tmp);
        }
    };
    /* 4 LDX ABY */
    static opcode m6502_de = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = DEC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 DEC ABX */
    static opcode m6502_fe = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 7;
            int tmp = RD_ABX();
            int tmp2 = INC(tmp);
            WB_EA(tmp2);
        }
    };
    /* 7 INC ABX */

    static opcode m6502_0f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_2f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_4f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_6f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_8f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_af = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_cf = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_ef = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

    static opcode m6502_1f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_3f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_5f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_7f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_9f = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_bf = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_df = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */
    static opcode m6502_ff = new opcode() {
        public void handler() {
            m6502_ICount[0] -= 2;
            ILL();
        }
    };
    /* 2 ILL */

 /* and here's the array of function pointers */
    public static opcode[] insn6502 = {
        m6502_00, m6502_01, m6502_02, m6502_03, m6502_04, m6502_05, m6502_06, m6502_07,
        m6502_08, m6502_09, m6502_0a, m6502_0b, m6502_0c, m6502_0d, m6502_0e, m6502_0f,
        m6502_10, m6502_11, m6502_12, m6502_13, m6502_14, m6502_15, m6502_16, m6502_17,
        m6502_18, m6502_19, m6502_1a, m6502_1b, m6502_1c, m6502_1d, m6502_1e, m6502_1f,
        m6502_20, m6502_21, m6502_22, m6502_23, m6502_24, m6502_25, m6502_26, m6502_27,
        m6502_28, m6502_29, m6502_2a, m6502_2b, m6502_2c, m6502_2d, m6502_2e, m6502_2f,
        m6502_30, m6502_31, m6502_32, m6502_33, m6502_34, m6502_35, m6502_36, m6502_37,
        m6502_38, m6502_39, m6502_3a, m6502_3b, m6502_3c, m6502_3d, m6502_3e, m6502_3f,
        m6502_40, m6502_41, m6502_42, m6502_43, m6502_44, m6502_45, m6502_46, m6502_47,
        m6502_48, m6502_49, m6502_4a, m6502_4b, m6502_4c, m6502_4d, m6502_4e, m6502_4f,
        m6502_50, m6502_51, m6502_52, m6502_53, m6502_54, m6502_55, m6502_56, m6502_57,
        m6502_58, m6502_59, m6502_5a, m6502_5b, m6502_5c, m6502_5d, m6502_5e, m6502_5f,
        m6502_60, m6502_61, m6502_62, m6502_63, m6502_64, m6502_65, m6502_66, m6502_67,
        m6502_68, m6502_69, m6502_6a, m6502_6b, m6502_6c, m6502_6d, m6502_6e, m6502_6f,
        m6502_70, m6502_71, m6502_72, m6502_73, m6502_74, m6502_75, m6502_76, m6502_77,
        m6502_78, m6502_79, m6502_7a, m6502_7b, m6502_7c, m6502_7d, m6502_7e, m6502_7f,
        m6502_80, m6502_81, m6502_82, m6502_83, m6502_84, m6502_85, m6502_86, m6502_87,
        m6502_88, m6502_89, m6502_8a, m6502_8b, m6502_8c, m6502_8d, m6502_8e, m6502_8f,
        m6502_90, m6502_91, m6502_92, m6502_93, m6502_94, m6502_95, m6502_96, m6502_97,
        m6502_98, m6502_99, m6502_9a, m6502_9b, m6502_9c, m6502_9d, m6502_9e, m6502_9f,
        m6502_a0, m6502_a1, m6502_a2, m6502_a3, m6502_a4, m6502_a5, m6502_a6, m6502_a7,
        m6502_a8, m6502_a9, m6502_aa, m6502_ab, m6502_ac, m6502_ad, m6502_ae, m6502_af,
        m6502_b0, m6502_b1, m6502_b2, m6502_b3, m6502_b4, m6502_b5, m6502_b6, m6502_b7,
        m6502_b8, m6502_b9, m6502_ba, m6502_bb, m6502_bc, m6502_bd, m6502_be, m6502_bf,
        m6502_c0, m6502_c1, m6502_c2, m6502_c3, m6502_c4, m6502_c5, m6502_c6, m6502_c7,
        m6502_c8, m6502_c9, m6502_ca, m6502_cb, m6502_cc, m6502_cd, m6502_ce, m6502_cf,
        m6502_d0, m6502_d1, m6502_d2, m6502_d3, m6502_d4, m6502_d5, m6502_d6, m6502_d7,
        m6502_d8, m6502_d9, m6502_da, m6502_db, m6502_dc, m6502_dd, m6502_de, m6502_df,
        m6502_e0, m6502_e1, m6502_e2, m6502_e3, m6502_e4, m6502_e5, m6502_e6, m6502_e7,
        m6502_e8, m6502_e9, m6502_ea, m6502_eb, m6502_ec, m6502_ed, m6502_ee, m6502_ef,
        m6502_f0, m6502_f1, m6502_f2, m6502_f3, m6502_f4, m6502_f5, m6502_f6, m6502_f7,
        m6502_f8, m6502_f9, m6502_fa, m6502_fb, m6502_fc, m6502_fd, m6502_fe, m6502_ff
    };

    public abstract interface opcode {

        public abstract void handler();
    }

}
