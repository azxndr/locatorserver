package lv.div.locator.conf;

import lv.div.locator.commons.conf.ConfigurationKey;
import lv.div.locator.commons.conf.Const;
import lv.div.locator.dao.ConfigurationDao;
import lv.div.locator.dao.StateDao;
import lv.div.locator.healthcheck.AlertSender;
import lv.div.locator.model.Configuration;
import lv.div.locator.model.mlsfences.JsonHelper;
import lv.div.locator.model.mlsfences.SafeAreas;
import org.joda.time.DateTime;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Stateless
public class ConfigurationManager {

    @EJB
    private ConfigurationDao configuration;

    @EJB
    private StateDao stateDao;

    @Inject
    private AlertSender alertSender;

    private Logger log = Logger.getLogger(ConfigurationManager.class.getName());

    public void loadDeviceSpecificConfigIfNeeded(String deviceId) {
        if (Conf.getInstance().deviceValues.get(deviceId) == null) {
            final List<Configuration> configurationList = configuration.listConfigByDeviceId(deviceId);
            loadDeviceSpecificConfiguration(deviceId, configurationList);
        }
    }

    public void loadDeviceSpecificConfiguration(String deviceId, List<Configuration> configurationList) {
        loadGlobalConfiguration();
        if (null != configurationList && configurationList.size() > 0) {

            Map<ConfigurationKey, Configuration> deviceConfig = new HashMap<ConfigurationKey, Configuration>();
            JsonHelper jsonHelper = new JsonHelper();

            for (Configuration conf : configurationList) {
                if (ConfigurationKey.DEVICE_MLS_FENCES.toString().equals(conf.getKey().toString())) {
                    // Handle complex configuration for MLS_FENCES:
                    final String jsonForSafeAreas = conf.getValue();
                    final SafeAreas safeAreas = (SafeAreas) jsonHelper.buildPojo(jsonForSafeAreas, SafeAreas.class);
                    conf.setSafeAreas(safeAreas);
                    deviceConfig.put(conf.getKey(), conf);

                    log.info("Parsed DEVICE_MLS_FENCES = "+jsonForSafeAreas);
                } else {
                    deviceConfig.put(conf.getKey(), conf);
                }
            }

            Conf.getInstance().deviceValues.put(deviceId, deviceConfig);

            final String alias =
                Conf.getInstance().deviceValues.get(deviceId).get(ConfigurationKey.DEVICE_ALIAS).getValue();

            final String afterMinutes =
                Conf.getInstance().deviceValues.get(deviceId).get(ConfigurationKey.DEVICE_RELOAD_CONFIG_MINUTES)
                    .getValue();

            DateTime now = new DateTime();
            final int updateHour = now.plusMinutes(Integer.parseInt(afterMinutes)).getHourOfDay();
            final int updateMinute = now.plusMinutes(Integer.parseInt(afterMinutes)).getMinuteOfHour();

            String logMessage = " Device \"" + alias +
                                "\" has [re]loaded configuration. Next reload after " + afterMinutes + "min., ~" +
                                String.format(Const.TIME_VALUE_FORMAT, updateHour) + ":" +
                                String.format(Const.TIME_VALUE_FORMAT, updateMinute);

            log.info(logMessage);

        }
    }

    public void loadGlobalConfiguration() {
        fetchGlobalConfiguration();
    }

    public void fetchGlobalConfiguration() {
        if (Conf.getInstance().globals.isEmpty()) {
            stateDao.cleanupAllStates(); // Cleanup all states at startup
            log.info("No configuration entries cached. Loading:");
            final List<Configuration> resultList = configuration.listConfigByDeviceId("*");
            for (Configuration conf : resultList) {
                log.info("Loaded ::: " + conf.getKey().toString());
                Conf.getInstance().globals.put(conf.getKey(), conf);
            }
        }
    }
}
