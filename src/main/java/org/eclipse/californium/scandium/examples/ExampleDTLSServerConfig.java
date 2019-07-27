package org.eclipse.californium.scandium.examples;

import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;

import com.beust.jcommander.Parameter;

/**
 * Contains the minimal subset of parameters which one should be able to toy around with when testing the scandium DTLS server.
 */
public class ExampleDTLSServerConfig {
	private static final String DEFAULT_TRUST_STORE_PASSWORD = "student";
	private static final String DEFAULT_TRUST_STORE_LOCATION = "rsa2048.jks";
	private static final String DEFAULT_TRUST_STORE_ALIAS = "tls-attacker";
	private static final int DEFAULT_PORT = 20000; 
	
	@Parameter(names = "-port", required = false, description = "The port the server is listening to")
	private Integer port = DEFAULT_PORT;
	
	@Parameter(names = "-trustLocation", required = false, description = "The localtion of the trust store to use")
	private String trustLocation = DEFAULT_TRUST_STORE_LOCATION;
	
	@Parameter(names = "-trustAlias", required = false, description = "The alias looked up to gather certs from the trust store")
	private String trustAlias = DEFAULT_TRUST_STORE_ALIAS;
	
	@Parameter(names = "-trustPassword", required = false, description = "The password with which the trust store is protected")
	private String trustPassword = DEFAULT_TRUST_STORE_PASSWORD;
	
	@Parameter(names = "-pskKey", converter=HexStringToBytesConverter.class, required = false, description = "The password (in hex form without the prefix 0x) with which the trust store is protected")
	private byte [] pskKey = new byte [] {0x12, 0x34}; 

	@Parameter(names = "-pskIdentity", required = false, description = "The psk identity to use")
	public String pskIdentity = "Client_identity";
	
	@Parameter(names = "-timeout", required = false, description = "The retransmission timeout for the Scandium DTLS implementation")
	private Integer timeout = 20000;
	
	@Parameter(names = "-cipherSuites", required = false, description = "The cipher suites to use")
	private List<CipherSuite> cipherSuites = Arrays.asList(CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256,
					CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256);
	
	@Parameter(names = "-reqCert", required = false, description = "Request client certificates")
	private boolean reqCert = false;
	
	@Parameter(names = "-help", required = false, description = "Prints usage")
	private boolean help = false;
	
	@Parameter(names = "-continuous", required = false, description = "Listens for clients in an infinite loop, otherwise terminates on processing the first client")
	private boolean continuous = false;
	
	public String getTrustLocation() {
		return trustLocation;
	}

	public String getTrustAlias() {
		return trustAlias;
	}

	public String getTrustPassword() {
		return trustPassword;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public boolean isReqCert() {
		return reqCert;
	}
	
	public boolean isHelp() {
		return help;
	}

	public int getPort() {
		return port;
	}
	
	public List<CipherSuite> getCipherSuites() {
		return cipherSuites;
	}
	
	
	public byte[] getPskKey() {
		return pskKey;
	}


	public String getPskIdentity() {
		return pskIdentity;
	}
	
	public boolean isContinuous() {
		return continuous;
	}

	
}
