package org.huel.cloudhub.meta.conf;

import org.huel.cloudhub.conf.AbstractConfigLoader;
import org.huel.cloudhub.meta.server.MetaServerApplication;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author RollW
 */
public class MetaConfigLoader extends AbstractConfigLoader {
    public static final String RPC_PORT_DEFAULT = "7031";
    public static final String RPC_MAX_INBOUND_SIZE_DEFAULT = "40";

    public static final String WEB_PORT_DEFAULT = "7030";

    public static final String FILE_TEMP_PATH_DEFAULT = "tmp";
    public static final String FILE_UPLOAD_BLOCK_SIZE_DEFAULT = "64";
    public static final String HEARTBEAT_STANDARD_PERIOD_DEFAULT = "500";
    public static final String HEARTBEAT_TIMEOUT_CYCLE_DEFAULT = "3";

    public MetaConfigLoader(InputStream inputStream) throws IOException {
        super(inputStream);
    }


    public int getRpcPort() {
        return getInt(MetaConfigKeys.RPC_PORT, RPC_PORT_DEFAULT);
    }

    public int getWebPort() {
        return getInt(MetaConfigKeys.WEB_PORT, WEB_PORT_DEFAULT);
    }

    public String getFileTempPath() {
        return get(MetaConfigKeys.FILE_TEMP_PATH, FILE_TEMP_PATH_DEFAULT);
    }

    public int getRpcMaxInboundSize() {
        return getInt(MetaConfigKeys.RPC_MAX_INBOUND_SIZE,
                RPC_MAX_INBOUND_SIZE_DEFAULT);
    }


    public int getHeartbeatStandardPeriod() {
        return getInt(MetaConfigKeys.HEARTBEAT_STANDARD_PERIOD, HEARTBEAT_STANDARD_PERIOD_DEFAULT);
    }

    public int getHeartbeatTimeoutCycle() {
        return getInt(MetaConfigKeys.HEARTBEAT_TIMEOUT_CYCLE, HEARTBEAT_TIMEOUT_CYCLE_DEFAULT);
    }

    public int getUploadBlockSize() {
        return getInt(MetaConfigKeys.FILE_UPLOAD_BLOCK_SIZE, FILE_UPLOAD_BLOCK_SIZE_DEFAULT);
    }

    public static MetaConfigLoader tryOpenDefault() throws IOException {
        return new MetaConfigLoader(
                openConfigInput(MetaServerApplication.class));
    }
}
