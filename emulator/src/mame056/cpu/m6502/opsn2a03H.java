/**
 * ported to v0.56
 */
package mame056.cpu.m6502;

import static mame056.cpu.m6502.ops02H.*;
import static mame056.cpu.m6502.m6502.*;

public class opsn2a03H {

    /* N2A03 *******************************************************
    *	ADC Add with carry - no decimal mode
    ***************************************************************/
    public static void ADC_NES(int tmp) {
        int c = (m6502.u8_p & F_C);
        int sum = m6502.u8_a + tmp + c;
        m6502.u8_p &= ((F_V | F_C) ^ 0xFFFFFFFF);
         if (((m6502.u8_a ^ tmp ^ 0xFFFFFFFF) & (m6502.u8_a ^ sum) & F_N) != 0) {
            m6502.u8_p |= F_V;
        }
        if ((sum & 0xff00) != 0) {
            m6502.u8_p |= F_C;
        }
        m6502.u8_a = sum & 0xFF;

        SET_NZ(m6502.u8_a);
    }

    /* N2A03 *******************************************************
    *	SBC Subtract with carry - no decimal mode
    ***************************************************************/
    public static void SBC_NES(int tmp) {
        int c = (m6502.u8_p & F_C) ^ F_C;
        int sum = m6502.u8_a - tmp - c;
        m6502.u8_p &= ((F_V | F_C) ^ 0xFFFFFFFF);
        if (((m6502.u8_a ^ tmp) & (m6502.u8_a ^ sum) & F_N) != 0) {
            m6502.u8_p |= F_V;
        }
        if ((sum & 0xff00) == 0) {
            m6502.u8_p |= F_C;
        }
        m6502.u8_a = sum & 0xFF;

        SET_NZ(m6502.u8_a);
    }
}
