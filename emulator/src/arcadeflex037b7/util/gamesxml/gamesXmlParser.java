package arcadeflex037b7.util.gamesxml;

import static common.libc.cstdio.sprintf;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import static mame056.driver.drivers;
import static mame056.driverH.*;
import static mame056.cpuexecH.*;
import mame056.driverH.MachineDriver;
import static mame056.sndintrf.*;
import static mame056.sndintrfH.*;
import static mame056.cpuintrf.cputype_name;

/**
 *
 * @author shadow
 */
public class gamesXmlParser {

    public static void writeGamesXml() {
        File f;
        f = new File("flexgames.xml");
        gamesRoot groot = new gamesRoot();
        List<game> games = new ArrayList<>();
        int i = 0;
        while (drivers[i] != null) {
            if ((drivers[i].flags & NOT_A_DRIVER) == 0) {
                game g = new game();
                g.setName(drivers[i].name);
                g.setDescription(drivers[i].description);

                MachineDriver x_driver = drivers[i].drv;
                MachineCPU[] x_cpu = x_driver.cpu;
                MachineSound[] x_sound = x_driver.sound;
                String cpu = "";
                for (int j = 0; j < MAX_CPU; j++) {
                    if ((x_cpu[j].cpu_type & CPU_AUDIO_CPU) != 0) {
                        String c = sprintf("[%-6s] ", cputype_name(x_cpu[j].cpu_type)).trim();
                        if (c.length() > 0) {
                            cpu += c;
                            cpu += ",";
                        }
                    } else {
                        String c = sprintf("%-8s ", cputype_name(x_cpu[j].cpu_type)).trim();
                        if (c.length() > 0) {
                            cpu += c;
                            cpu += ",";
                        }
                    }
                }
                if (cpu.length() > 1) {
                    g.setCpus(cpu.substring(0, cpu.length() - 1));
                }
                String sounds = "";
                for (int j = 0; j < MAX_SOUND; j++) {
                    if (sound_num(x_sound[j]) != 0) {
                        String s = sprintf("%dx%-9s", sound_num(x_sound[j]), sound_name(x_sound[j])).trim();
                        if (s.length() > 0) {
                            sounds += s;
                            sounds += ",";
                        }
                    } else {
                        String s = sprintf("%-11s ", sound_name(x_sound[j])).trim();
                        if (s.length() > 0) {
                            sounds += s;
                            sounds += ",";
                        }
                    }
                }
                if (sounds.length() > 1) {
                    g.setSounds(sounds.substring(0, sounds.length() - 1));
                }
                games.add(g);

                if ((drivers[i].flags & GAME_NOT_WORKING) != 0) {
                    g.setWorking("NO");
                } else if ((drivers[i].flags & GAME_UNEMULATED_PROTECTION) != 0) {
                    g.setWorking("NO");
                } else {
                    g.setWorking("YES");
                }
                if ((drivers[i].flags & GAME_WRONG_COLORS) != 0) {
                    g.setCorrectColors("NO");
                } else if ((drivers[i].flags & GAME_IMPERFECT_COLORS) != 0) {
                    g.setCorrectColors("CLOSE");
                } else {
                    g.setCorrectColors("YES");
                }
                if ((drivers[i].flags & GAME_NO_SOUND) != 0) {
                    g.setSound("NO");
                } else if ((drivers[i].flags & GAME_IMPERFECT_SOUND) != 0) {
                    g.setSound("PARTIAL");
                } else {
                    g.setSound("YES");
                }
                if ((drivers[i].flags & GAME_NO_COCKTAIL) != 0) {
                    g.setScreenflip("NO");
                } else {
                    g.setScreenflip("YES");
                }
            }
            i++;
        }
        try {
            groot.setFile(games);
            JAXBContext jaxbContext = JAXBContext.newInstance(gamesRoot.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(groot, f);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }
}
