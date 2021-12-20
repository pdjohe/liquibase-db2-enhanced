package liquibase.change.ext.db2.enhanced;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public abstract class AbstractTest {

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @BeforeEach
    void init(TestInfo testInfo) {
        logger.info("Running test: {}", testInfo.getDisplayName());
    }

}
