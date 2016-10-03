package se.redmind.rmtest.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import se.redmind.rmtest.WebDriverWrapper;
import se.redmind.rmtest.selenium.grid.DescriptionBuilder;
import se.redmind.rmtest.selenium.grid.GridWebDriver;
import se.redmind.rmtest.selenium.grid.HubNodesStatus;

import javax.validation.constraints.NotNull;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppiumGridConfiguration extends DriverConfiguration<RemoteWebDriver> {

    @JsonProperty
    @NotNull
    public String hubIp;

    @JsonProperty
    public int hubPort = 4444;

    @JsonProperty
    public boolean enableLiveStream;

    public AppiumGridConfiguration() {
        super(new DesiredCapabilities());
    }

    @Override
    protected List<WebDriverWrapper<RemoteWebDriver>> createDrivers() {
        List<WebDriverWrapper<RemoteWebDriver>> instances = new ArrayList<>();
        HubNodesStatus nodeInfo = new HubNodesStatus(hubIp, hubPort);
        nodeInfo.getNodesAsRegReqs().forEach(nodeReq -> {
            nodeReq.getCapabilities().stream()
                    .map(capabilities -> new DesiredCapabilities(capabilities))
                    .forEach(capabilities -> {
                        try {
                            String driverDescription = DescriptionBuilder.buildDescriptionFromCapabilities(capabilities);
                            URL driverUrl = new URL("http://" + nodeReq.getConfigAsString("host") + ":" + nodeReq.getConfigAsString("port") + "/wd/hub");
                            generateCapabilities().asMap().forEach((key, value) -> capabilities.setCapability(key, value));
                            instances.add(new WebDriverWrapper<>(capabilities, driverDescription, (otherCapabilities) -> {
                                return createRemoteWebDriver(driverUrl, otherCapabilities);
                            }));
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    });
        });
        return instances;
    }

    public RemoteWebDriver createRemoteWebDriver(URL remoteAddress, Capabilities desiredCapabilities) {
        String platformName = (String) desiredCapabilities.getCapability("platformName");
        if (null != platformName) {
            List<String> elements = Arrays.asList(platformName.toUpperCase().split("\\s"));
            if (elements.contains("ANDROID")) {
                return new AndroidDriver<>(remoteAddress, desiredCapabilities);
            }
            if (elements.contains("IOS")) {
                return new IOSDriver<>(remoteAddress, desiredCapabilities);
            }
        }
        return new GridWebDriver(remoteAddress, desiredCapabilities);
    }
}
