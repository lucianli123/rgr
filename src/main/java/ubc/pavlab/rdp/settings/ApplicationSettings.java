package ubc.pavlab.rdp.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.services.UserService;

/**
 * Created by mjacobson on 22/01/18.
 */
@Component
@ConfigurationProperties(prefix = "rdp.settings")
@Getter
@Setter
public class ApplicationSettings {

    @Getter
    @Setter
    public static class CacheSettings {

        private boolean enabled = true;
        private boolean loadFromDisk;
        private String geneFilesLocation;
        private String termFile;
        private String annotationFile;

    }

    @SuppressWarnings("WeakerAccess") //Used in frontend
    @Getter
    @Setter
    public static class PrivacySettings {
        private Integer defaultLevel = UserService.PRIVACY_PRIVATE;
        private Integer minLevel = UserService.PRIVACY_PRIVATE;
        private boolean defaultSharing = false;
        private boolean customizableLevel = true;
        private boolean customizableSharing = true;
        private boolean publicSearch = false;
        private boolean registeredSearch = false;
        private boolean allowHideGenelist = false;
    }

    @SuppressWarnings("WeakerAccess") //Used in frontend
    @Getter
    @Setter
    public static class InternationalSearchSettings {
        private boolean enabled = false;
        private boolean defaultOn = false;
        private String[] apis;
    }

    private InternationalSearchSettings isearch;
    private PrivacySettings privacy;
    private CacheSettings cache;
    private boolean sendEmailOnRegistration;
    private int goTermSizeLimit = 100;

}
