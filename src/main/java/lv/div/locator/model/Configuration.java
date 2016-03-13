package lv.div.locator.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.sql.Timestamp;

/**
 * Settings table
 */
@Entity
@Table(
    name="configuration",
    uniqueConstraints = @UniqueConstraint(name="unq_conf_key", columnNames = {"deviceid", "ckey"})
)
public class Configuration extends ConfigurationBased{

    public Configuration() {
    }
}
