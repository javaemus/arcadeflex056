package arcadeflex037b7.util.gamesxml;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author shadow
 */
@XmlRootElement(name = "games")
@XmlAccessorType(XmlAccessType.FIELD)
public class gamesRoot {

    public gamesRoot() {
        file = new ArrayList<>();
    }
    @XmlElement(name = "game")
    private List<game> file;

    public List<game> getFile() {
        return file;
    }

    public void setFile(List<game> file) {
        this.file = file;
    }
}
