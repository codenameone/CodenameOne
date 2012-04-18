/**
 * This file is public domain see
 * http://code.google.com/p/codenameone/issues/detail?id=148
 * http://supportforums.blackberry.com/t5/Java-Development/Sample-HTTP-Connection-code-and-BIS-B-Access/td-p/653175
 * Authored and contributed by Peter Strange
 */
package com.codename1.impl.blackberry;




import net.rim.device.api.servicebook.ServiceBook;
import net.rim.device.api.servicebook.ServiceRecord;
import net.rim.device.api.synchronization.ConverterUtilities;
import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.util.DataBuffer;
import net.rim.device.api.util.TLEUtilities;

public class TransportDetective {
    /**
     * CoverageInfo.COVERAGE_DIRECT is only available in 4.5 or newer
     */
    private static final int COVERAGE_DIRECT = 1;
    
        // ServiceRecord for detected transports in the service book
        ServiceRecord srTcpCellularWithApn, srWap, srWap2, srMds, srBis, srTcpWiFi;
        // Transport type constants
        public static final int TRANSPORT_TCP_CELLULAR                          = 1;            // Represents TCP Cellular transport also known as Direct TCP
    public static final int TRANSPORT_WAP                                               = 2;            // Represents the Wap 1.0 and Wap 1.1 transport types
    public static final int TRANSPORT_WAP2                                              = 4;            // Represents the Wap 2.0 transport type
    public static final int TRANSPORT_MDS                                               = 8;            // Represents the MDS transport type
    public static final int TRANSPORT_BIS_B                                     = 16;           // Represents the Blackberry Internet Service transport type
    public static final int TRANSPORT_TCP_WIFI                                  = 32;           // Represents the WIFI transport type
    public static final int TCP_CELLULAR_APN_SERVICE_BOOK               = 64;           // Represents the availability of TCP_CELLULAR service book that has APN information pre-populated. Please note that this is not a new transport. This simply indicates that your application do  not need to know the APN information for TCP_CELLULAR since the device already knows it. Please see URLFactory.getHttpTcpCellularUrlUsingServiceRecord(ServiceRecord tcpServiceRecord)   
    
    // Following descriptins added so that options can be displayed to users
    public static String TRANSPORT_TCP_CELLULAR_DESCRIPTION = "Carrier";
    public static String TRANSPORT_WAP_DESCRIPTION = "Wap 1";
    public static String TRANSPORT_WAP2_DESCRIPTION = "Wap 2";
    public static String TRANSPORT_MDS_DESCRIPTION = "BES/MDS";
    public static String TRANSPORT_BIS_B_DESCRIPTION = "BIS-B";
    public static String TRANSPORT_TCP_WIFI_DESCRIPTION = "WiFi";
    public static String TRANSPORT_TCP_CELLULAR_APN_DESCRIPTION = "Carrier (Preconfigured)";
    
    // Constants used for TRANSPORT_BIS_B and TRANSPORT_MDS type detection
    private static final byte SERVICE_RECORD_TYPE_IPPP_TAG               = 0x06;
    private static final byte SERVICE_RECORD_TYPE_IPPP_CORPORATE         = 0x00;
    private static final byte SERVICE_RECORD_TYPE_IPPP_PUBLIC            = 0x01;
    private static final byte SERVICE_RECORD_TYPE_IPPP_PROVISIONING      = 0x02;

    // Constants used for TRANSPORT_TCP_WIFI and TRANSPORT_WAP2 type detection
    private static final byte ENCODED_TYPE_INTERFACE                            = 0x13;
    private static final byte ENCODED_TYPE_MMSC_URL                             = 0x0D;
    private static final byte ENCODED_TYPE_HTTP_PROXY_ADDRESS           = 0x01; 
    private static final byte ENCODED_TYPE_PROXY_AUTH_USERNAME_TYPE = 0x09;
    private static final byte AUTH_TYPE_BBAUTH_TOKEN_NEGOTIATION        = 0x1C;
    // Interface name string of TCP WiFi transport service book.
    private static final String SERVICE_RECORD_TYPE_WPTCP_INTERFACE_WIFI = "wifi";
    // Interface name string of TCP Cellular transport service book. Commented because it is not used at present
    //private static final String SERVICE_RECORD_TYPE_WPTCP_INTERFACE_CELLULAR     = "cellular";

    // Constant used for TRANSPORT_WAP type detection
    private static final byte SERVICE_RECORD_TYPE_WAP_MMSC_URL_TAG       = 0x09;
    
    // Mask representing available transport service books
    private int _availableTransportServices = 0;
    // Mask representing available transport coverage
    private int _availableTransportCoverage = 0;    
    
    private ServiceBook _serviceBook;
    
    public TransportDetective(){
        _serviceBook = ServiceBook.getSB();
    }
    
    
    /**
     * Determines if a specific transport is available based on service book availability. In other words this method will return true only
     * for those available transports for which there is a service book present on the device. This does not guarantee connection capability.
     * You also need sufficient coverage for a transport to work.
     * @param transport One of TRANSPORT_* constants.
     * @return  True if available. Otherwise false.
     */
    public synchronized boolean isTransportServiceAvailable(int transport){
        int available = getAvailableTransportServices();
        if((available & transport) > 0)
                return true;
        else 
                return false;           
    }
    
    /**
     * Determines if a specific transport is available based on connectivity and service availability. In other words this method will return true only
     * for those available transports for which there is sufficient coverage on the device and there is a service book present. Although having a transport
     * in coverage means that we can attempt to create a connection over this transport, connections can still fail due to other network factors (e.g. server
     * outage)
     * @param transport One of TRANSPORT_* constants.
     * @return  True if available. Otherwise false.
     */
    public synchronized boolean isCoverageAvailable(int transport){
        int available = getAvailableTransportCoverage();
        if((available & transport) > 0)
                return true;
        else 
                return false;           
    }

    
    /**
     * Gets available transports for which a service book is present.
     * @return  Available transports based on service book.
     */
    public synchronized int getAvailableTransportServices(){
        updateTransportServiceAvailability();           
        return _availableTransportServices;
    }
    
    /**
     * Gets available transports for which coverage is present.
     * @return  Available transports based on coverage.
     */
    public synchronized int getAvailableTransportCoverage(){
        updateTransportCoverageAvailability();
        return _availableTransportCoverage;
    }
   
    /**
     * Determines if service record for TCP Cellular APN is present. If yes connections via TCP_CELLULAR can be attempted without knowing
     * the APN information for carriers.
     * @return  true, if present. false otherwise.
     */
    public boolean isTcpCellularApnServiceRecordAvailable(){
        updateTransportServiceAvailability();
        return ((_availableTransportServices & TCP_CELLULAR_APN_SERVICE_BOOK) > 0);
    }
    
    /** Getters for ServiceRecord of each transport */
 
        public ServiceRecord getSrWap() {
                return srWap;
        }

        public ServiceRecord getSrWap2() {
                return srWap2;
        }

        public ServiceRecord getSrMds() {
                return srMds;
        }

        public ServiceRecord getSrBis() {
                return srBis;
        }

        public ServiceRecord getSrTcpWiFi() {
                return srTcpWiFi;
        }
        
        public ServiceRecord getSrTcpCellularWithApn() {
                return srTcpCellularWithApn;
        }
        

        /**
     * Updates available transport service availability. 
     */
        private void updateTransportServiceAvailability() {

        // reset cached availability        
        _availableTransportServices = 0;

        ServiceRecord[] records = _serviceBook.getRecords();
        boolean cellularDataServiceSupported = isCellularDataServiceSupported();
        if ( cellularDataServiceSupported ) {
            _availableTransportServices |= TRANSPORT_TCP_CELLULAR;
        }
        boolean wifiSupported = isWifiSupported();

        for( int i = 0; i < records.length; i++ ) {
            ServiceRecord serviceRecord = records[ i ];
            if( serviceRecord.isValid() && ( serviceRecord.getType() == ServiceRecord.SRT_ACTIVE ) ) {
                int transportType = determineTransportType( serviceRecord );
                if (( transportType != -1 ) ) {
                    if (   (( transportType == TRANSPORT_MDS || ( transportType & (TRANSPORT_BIS_B)) > 0 )
                            && ( cellularDataServiceSupported || wifiSupported ))
                        || (( transportType == TRANSPORT_WAP || transportType == TRANSPORT_WAP2 )
                                && ( cellularDataServiceSupported ))
                        || (( transportType == TRANSPORT_TCP_WIFI )
                                && ( wifiSupported ))   
                        || (( transportType == TCP_CELLULAR_APN_SERVICE_BOOK)
                                        && ( cellularDataServiceSupported ))        ) {        
                        _availableTransportServices |= transportType ;
                    }                    
                }
            }
        }               
    }
        
    /**
     * Updates transport coverage availability.
     */
    private void updateTransportCoverageAvailability() {
        // reset cached availability

        // In 3G network it works...
        _availableTransportCoverage = 0;

        // Required for 2G networks
        int _coverageStatus = CoverageInfo.getCoverageStatus();

        if ((_coverageStatus & COVERAGE_DIRECT) > 0
                || CoverageInfo.isCoverageSufficient(COVERAGE_DIRECT, RadioInfo.getSupportedWAFs(), false)) {
            _availableTransportCoverage |= TRANSPORT_TCP_CELLULAR;
            _availableTransportCoverage |= TRANSPORT_WAP2;
            _availableTransportCoverage |= TRANSPORT_WAP;
        }

        if ((_coverageStatus & CoverageInfo.COVERAGE_BIS_B) > 0
                || CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_BIS_B)
                || CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_BIS_B, RadioInfo.getSupportedWAFs(), false)) {
            _availableTransportCoverage |= TRANSPORT_BIS_B;
        }
        if ((_coverageStatus & CoverageInfo.COVERAGE_MDS) > 0
                || CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_MDS)
                || CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_MDS, RadioInfo.getSupportedWAFs(), false)) {
            _availableTransportCoverage |= TRANSPORT_MDS;
        }
        if (CoverageInfo.isCoverageSufficient(COVERAGE_DIRECT, RadioInfo.WAF_WLAN, false)) {
            _availableTransportCoverage |= TRANSPORT_TCP_WIFI;
        }

        updateTransportServiceAvailability();
        _availableTransportCoverage &= _availableTransportServices;

    }
        
    /**
     * Determines if Data Service is enabled via Cellular radio.
     * 
     * @return <b>true</b> if enabled. <b>false</b> otherwise.
     */
    public boolean isCellularDataServiceSupported() {
        return ((RadioInfo.getSupportedWAFs() & (RadioInfo.WAF_3GPP | RadioInfo.WAF_CDMA | RadioInfo.WAF_IDEN)) > 0);
    }
        
        /**
         * Determines if WiFi is supported.
         * @return      <b>true</b> if enabled. <b>false</b> otherwise.
         */
        private boolean isWifiSupported() {
        return ( (RadioInfo.getSupportedWAFs() & RadioInfo.WAF_WLAN) > 0 );
    }
        
        /**
         * Given a service record, determines the transport it represents.
         * @param serviceRecord A service record.
         * @return      One of TRANSPORT_* constants. 
         */
        private int determineTransportType( ServiceRecord serviceRecord ) {

        String cid = serviceRecord.getCid();

        // MDS and BIS        
        if( "ippp".equalsIgnoreCase( cid )) {
            int ippType = getEncodedIntFromIPPPServiceRecord( serviceRecord, SERVICE_RECORD_TYPE_IPPP_TAG );
            
            if ( ippType == SERVICE_RECORD_TYPE_IPPP_PUBLIC ) {
                int result = 0;
                srBis = serviceRecord;
                result |= TRANSPORT_BIS_B;                
                return result;
            } else if (( ippType == SERVICE_RECORD_TYPE_IPPP_CORPORATE )
                    || ( ippType != SERVICE_RECORD_TYPE_IPPP_PROVISIONING )){
                srMds = serviceRecord;
                return TRANSPORT_MDS;
            }

        }

        // WiFi and WAP 2.0        
        if( "wptcp".equalsIgnoreCase( cid ) ) {
            String interfaceName = getEncodedStringFieldFromWptcpServiceRecord( serviceRecord,  ENCODED_TYPE_INTERFACE );
            
            if ( SERVICE_RECORD_TYPE_WPTCP_INTERFACE_WIFI.equals( interfaceName ) ) {
                srTcpWiFi = serviceRecord;
                return TRANSPORT_TCP_WIFI;
            } else {
                String mmscUrl = getEncodedStringFieldFromWptcpServiceRecord( serviceRecord, ENCODED_TYPE_MMSC_URL );           
                String httpProxyAddress = getEncodedStringFieldFromWptcpServiceRecord( serviceRecord, ENCODED_TYPE_HTTP_PROXY_ADDRESS );
                if ( ( mmscUrl == null) || ( mmscUrl.trim().length() == 0 ) ) {
                        if ( getEncodedIntFieldFromWptcpServiceRecord(serviceRecord, ENCODED_TYPE_PROXY_AUTH_USERNAME_TYPE ) != AUTH_TYPE_BBAUTH_TOKEN_NEGOTIATION ) {
                        if ( ( httpProxyAddress == null ) || ( httpProxyAddress.trim().length() == 0 ) ) {
                                srTcpCellularWithApn = serviceRecord;
                            return TCP_CELLULAR_APN_SERVICE_BOOK;
                        } else {
                                srWap2 = serviceRecord;
                            return TRANSPORT_WAP2;
                        }
                    }
                }
            }
        }

        // Wap1.0
       if( "wap".equalsIgnoreCase( cid ) ) {
            String mmscUrl = getEncodedStringFieldFromWapServiceRecord( serviceRecord, SERVICE_RECORD_TYPE_WAP_MMSC_URL_TAG );            
            if ( ( mmscUrl == null) || ( mmscUrl.trim().length() == 0 ) ) {
                srWap = serviceRecord;
                return TRANSPORT_WAP;
            }
        }
        
        return -1;
    }
        
        /**
     * Determines the int Value corresponding to the parameter tag from the IPPP Service Record's encoded Application Data
     *
     * @param ipppServiceRecord
     *            an IPPP ServiceRecord. Must not be null.
     *
     * @param typeByteCode
     *            a byte representing the IPPP Application Data specific parameter tag.
     *
     * @return an int representing the value assigned to the particular parameter tag or -1 if the encoded value is not found
     *
     */
    private static int getEncodedIntFromIPPPServiceRecord( ServiceRecord ipppServiceRecord, byte typeByteCode ) {       
        int intVal = -1;
        byte[] applicationData = ipppServiceRecord.getApplicationData();
        if( applicationData != null ) {
            DataBuffer buffer = new DataBuffer(applicationData, 0, applicationData.length, true);            
            try {
                if(ConverterUtilities.findType(buffer, typeByteCode)){                  
                        intVal = ConverterUtilities.readInt(buffer);
                }
            } catch( Throwable e ) {
                
            }
        }
        return intVal;
    }

    /**
     * Determines the String Value corresponding to the parameter tag from the WPTCP Service Record's encoded Application Data
     *
     * @param wptcpServiceRecord
     *            a WPTCP ServiceRecord. Must not be null.
     *
     * @param typeByteCode
     *            a byte representing the WPTCP Application Data specific parameter tag.
     *
     * @return a String representing the value assigned to the particular parameter tag or <b>null</b> if the encoded value does not exist.
     *
     */
    private static String getEncodedStringFieldFromWptcpServiceRecord( ServiceRecord wptcpServiceRecord, byte typeByteCode ) {
        String stringData = null;
        byte[] applicationData = wptcpServiceRecord.getApplicationData();
        if( applicationData != null ) {
            DataBuffer buffer = new DataBuffer( applicationData, 0, applicationData.length, true );
            try {
                // skip version
                buffer.readByte();
                if(TLEUtilities.findType(buffer, typeByteCode)){
                        stringData = TLEUtilities.readStringField(buffer, typeByteCode);
                }
            } catch( Throwable e ) {                
                
            }
        }
        return stringData;
    }
    
    /**
     * Determines the int Value corresponding to the parameter tag from the WPTCP Service Record's encoded Application Data
     *
     * @param wptcpServiceRecord
     *            a WPTCP ServiceRecord. Must not be null.
     *
     * @param typeByteCode
     *            a byte representing the WPTCP Application Data specific parameter tag.
     *
     * @return an int representing the value assigned to the particular parameter tag or <b>null</b> if the encoded value does not exist.
     *
     */
    private static int getEncodedIntFieldFromWptcpServiceRecord( ServiceRecord wptcpServiceRecord, byte typeByteCode ) {
        int intData = -1;
        byte[] applicationData = wptcpServiceRecord.getApplicationData();
        if( applicationData != null ) {
            DataBuffer buffer = new DataBuffer( applicationData, 0, applicationData.length, true );
            try {
                // skip version
                buffer.readByte();
                if(TLEUtilities.findType(buffer, typeByteCode)){
                        intData = TLEUtilities.readIntegerField(buffer, typeByteCode);
                }
            } catch( Throwable e ) {                
                
            }
        }
        return intData;
    }
    
    /**
     * Determines the String Value corresponding to the parameter tag from the WAP Service Record's encoded Application Data
     *
     * @param wapServiceRecord
     *            an WAP ServiceRecord 
     *            
     * @param typeByteCode
     *            a byte representing the WAP Application Data specific parameter tag
     *
     * @return a String representing the value assigned to the particular parameter tag or <b>null</b> if the encoded value does not exist.
     *
     */
    private static String getEncodedStringFieldFromWapServiceRecord( ServiceRecord wapServiceRecord, byte typeByteCode ) {
        String stringData = null;
        byte[] applicationData = wapServiceRecord.getApplicationData();
        if( applicationData != null ) {
            DataBuffer buffer = new DataBuffer( applicationData, 0, applicationData.length, true );            
            
            try {
                //skip version
                buffer.readByte();
                if(TLEUtilities.findType(buffer, typeByteCode)){
                        stringData = TLEUtilities.readStringField(buffer, typeByteCode);
                }
            } catch( Throwable e ) {
                
            }
        }
        return stringData;
    }
    
    // Переменна�? _target �?одержит URL
    public int getBestTransportForActiveCoverage() {
        // Требует�?�? определить, какой тран�?порт нам �?ейча�? до�?тупен...

        int availableTransports = getAvailableTransportCoverage();
        int iCurTransport = -1;
        if ((availableTransports & TransportDetective.TRANSPORT_TCP_WIFI) > 0) {
            iCurTransport = TransportDetective.TRANSPORT_TCP_WIFI;
        } else if (RadioInfo.getState() == RadioInfo.STATE_ON
                && RadioInfo.getSignalLevel() != RadioInfo.LEVEL_NO_COVERAGE) {

            if ((availableTransports & TransportDetective.TRANSPORT_MDS) > 0) {
                iCurTransport = TransportDetective.TRANSPORT_MDS;

            } else if ((availableTransports & TransportDetective.TRANSPORT_BIS_B) > 0) {
                iCurTransport = TransportDetective.TRANSPORT_BIS_B;
            }

            if (iCurTransport == -1 && ((availableTransports & TransportDetective.TCP_CELLULAR_APN_SERVICE_BOOK) > 0)) {
                iCurTransport = TransportDetective.TCP_CELLULAR_APN_SERVICE_BOOK;
            } else if (iCurTransport == -1 && ((availableTransports & TransportDetective.TRANSPORT_TCP_CELLULAR) > 0)) {
                iCurTransport = TransportDetective.TRANSPORT_TCP_CELLULAR;
            }
        }
        return iCurTransport;
    }
}
