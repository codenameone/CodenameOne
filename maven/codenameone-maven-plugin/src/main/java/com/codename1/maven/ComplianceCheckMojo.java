package com.codename1.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * @deprecated Renamed to the bytecode-compliance goal. This alias is kept for backward compatibility with older project POMs.
 */
@Deprecated
@Mojo(name = "compliance-check", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.TEST)
public class ComplianceCheckMojo extends BytecodeComplianceMojo {
}
