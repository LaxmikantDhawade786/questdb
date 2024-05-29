package io.questdb;

import io.questdb.cairo.CairoConfiguration;
import io.questdb.cairo.CairoEngine;
import io.questdb.cutlass.http.HttpMinServerConfiguration;
import io.questdb.cutlass.http.HttpServerConfiguration;
import io.questdb.cutlass.json.JsonException;
import io.questdb.cutlass.line.tcp.LineTcpReceiverConfiguration;
import io.questdb.cutlass.line.udp.LineUdpReceiverConfiguration;
import io.questdb.cutlass.pgwire.PGWireConfiguration;
import io.questdb.log.Log;
import io.questdb.metrics.MetricsConfiguration;
import io.questdb.mp.WorkerPoolConfiguration;
import io.questdb.std.FilesFacade;
import io.questdb.std.FilesFacadeImpl;
import io.questdb.std.datetime.microtime.MicrosecondClock;
import io.questdb.std.datetime.microtime.MicrosecondClockImpl;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;


public class DynamicPropServerConfiguration implements DynamicServerConfiguration {

    private final BuildInformation buildInformation;
    private final AtomicReference<PropServerConfiguration> delegate;
    private final @Nullable Map<String, String> env;
    private final FilesFacade filesFacade;
    private final FactoryProviderFactory fpf;
    private final boolean loadAdditionalConfigurations;
    private final Log log;
    private final MicrosecondClock microsecondClock;
    private final String root;
    private FileEventCallback fileEventCallback;

    public DynamicPropServerConfiguration(
            String root,
            Properties properties,
            @Nullable Map<String, String> env,
            Log log,
            BuildInformation buildInformation,
            FilesFacade filesFacade,
            MicrosecondClock microsecondClock,
            FactoryProviderFactory fpf,
            boolean loadAdditionalConfigurations
    ) throws ServerConfigurationException, JsonException, IOException {
        this.root = root;
        this.env = env;
        this.log = log;
        this.buildInformation = buildInformation;
        this.filesFacade = filesFacade;
        this.microsecondClock = microsecondClock;
        this.fpf = fpf;
        this.loadAdditionalConfigurations = loadAdditionalConfigurations;

        PropServerConfiguration serverConfig = new PropServerConfiguration(
                root,
                properties,
                env,
                log,
                buildInformation,
                filesFacade,
                microsecondClock,
                fpf,
                loadAdditionalConfigurations
        );
        this.delegate = new AtomicReference<>(serverConfig);
        this.fileEventCallback = new ConfigReloader(this);
    }

    public DynamicPropServerConfiguration(
            String root,
            Properties properties,
            @Nullable Map<String, String> env,
            Log log,
            final BuildInformation buildInformation,
            FilesFacade filesFacade,
            MicrosecondClock microsecondClock,
            FactoryProviderFactory fpf
    ) throws ServerConfigurationException, JsonException, IOException {
        this(
                root,
                properties,
                env,
                log,
                buildInformation,
                filesFacade,
                microsecondClock,
                fpf,
                true
        );
    }

    public DynamicPropServerConfiguration(
            String root,
            Properties properties,
            @Nullable Map<String, String> env,
            Log log,
            final BuildInformation buildInformation
    ) throws ServerConfigurationException, JsonException, IOException {
        this(
                root,
                properties,
                env,
                log,
                buildInformation,
                FilesFacadeImpl.INSTANCE,
                MicrosecondClockImpl.INSTANCE,
                (configuration, engine, freeOnExitList) -> DefaultFactoryProvider.INSTANCE,
                true
        );
    }

    public DynamicPropServerConfiguration(
            String root,
            Properties properties,
            @Nullable Map<String, String> env,
            Log log,
            BuildInformation buildInformation,
            FilesFacade filesFacade,
            MicrosecondClock microsecondClock,
            FactoryProviderFactory fpf,
            boolean loadAdditionalConfigurations,
            Function<DynamicPropServerConfiguration, FileEventCallback> fileEventCallbackFunction
    ) throws ServerConfigurationException, JsonException, IOException {
        this(
                root,
                properties,
                env,
                log,
                buildInformation,
                filesFacade,
                microsecondClock,
                fpf,
                loadAdditionalConfigurations
        );

        this.fileEventCallback = fileEventCallbackFunction.apply(this);
    }

    @Override
    public CairoConfiguration getCairoConfiguration() {
        return delegate.get().getCairoConfiguration();
    }

    @Override
    public FactoryProvider getFactoryProvider() {
        return delegate.get().getFactoryProvider();
    }

    @Override
    public FileEventCallback getFileEventCallback() {
        return this.fileEventCallback;
    }

    @Override
    public HttpMinServerConfiguration getHttpMinServerConfiguration() {
        return delegate.get().getHttpMinServerConfiguration();
    }

    @Override
    public HttpServerConfiguration getHttpServerConfiguration() {
        return delegate.get().getHttpServerConfiguration();
    }

    @Override
    public LineTcpReceiverConfiguration getLineTcpReceiverConfiguration() {
        return delegate.get().getLineTcpReceiverConfiguration();
    }

    @Override
    public LineUdpReceiverConfiguration getLineUdpReceiverConfiguration() {
        return delegate.get().getLineUdpReceiverConfiguration();
    }

    @Override
    public MemoryConfiguration getMemoryConfiguration() {
        return delegate.get().getMemoryConfiguration();
    }

    @Override
    public MetricsConfiguration getMetricsConfiguration() {
        return delegate.get().getMetricsConfiguration();
    }

    @Override
    public PGWireConfiguration getPGWireConfiguration() {
        return delegate.get().getPGWireConfiguration();
    }

    @Override
    public PublicPassthroughConfiguration getPublicPassthroughConfiguration() {
        return delegate.get().getPublicPassthroughConfiguration();
    }

    @Override
    public WorkerPoolConfiguration getWalApplyPoolConfiguration() {
        return delegate.get().getWalApplyPoolConfiguration();
    }

    @Override
    public WorkerPoolConfiguration getWorkerPoolConfiguration() {
        return delegate.get().getWorkerPoolConfiguration();
    }

    @Override
    public void init(CairoEngine engine, FreeOnExit freeOnExit) {
        delegate.get().init(this, engine, freeOnExit);
    }

    @Override
    public boolean isLineTcpEnabled() {
        return delegate.get().isLineTcpEnabled();
    }

    public void reload(Properties properties) {
        PropServerConfiguration newConfig;
        try {
            newConfig = new PropServerConfiguration(
                    this.root,
                    properties,
                    this.env,
                    this.log,
                    this.buildInformation,
                    this.filesFacade,
                    this.microsecondClock,
                    this.fpf,
                    this.loadAdditionalConfigurations
            );
        } catch (ServerConfigurationException | JsonException e) {
            this.log.error().$(e);
            return;
        }

        delegate.set(newConfig);
    }
}
