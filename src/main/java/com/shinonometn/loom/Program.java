package com.shinonometn.loom;

import com.shinonometn.loom.common.ConfigModule;
import com.shinonometn.loom.common.Networks;
import com.shinonometn.loom.common.Toolbox;
import com.shinonometn.loom.core.Shuttle;
import com.shinonometn.loom.resource.Resource;
import com.shinonometn.loom.ui.MainForm;
import com.shinonometn.Pupa.ToolBox.HexTools;
import org.apache.log4j.PropertyConfigurator;

import javax.swing.*;
import java.io.*;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 * Created by catten on 15/10/20.
 */
public class Program{

    public static boolean isDeveloperMode(){
        return System.getProperty("loom.developerMode").equals("t");
    }

    public final static String appName = "Loom v2.2";

    public static void main(String[] args){
        try { initLogger(); }catch (Throwable t){ System.out.println("Logger Initialization failed!"); }
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("main");

        System.setProperty("loom.mode","graphical");
        System.setProperty("loom.appName","Loom");
        System.setProperty("loom.version","v2.2");
        System.setProperty("loom.developerMode","f");

        String ip = null;

        if(args.length > 0){
            for (int i = 0; i < args.length; i++){
                switch (args[i].toLowerCase()){
                    case "-h":
                    case "--help":
                        System.out.println(System.getProperty("loom.appName") + " " + System.getProperty("loom.version"));
                        System.out.println(Resource.getResource().getResourceText("/com/shinonometn/loom/resource/text/help.txt"));
                        System.exit(0);
                        break;

                    case "-dm":
                    case "--developer-mode":
                        i++;
                        if(args[i].matches("(yes|y|true|t)")){
                            System.setProperty("loom.developerMode","t");
                        }
                        break;

                    case "-cm":
                    case "--console-mode":
                        System.setProperty("loom.mode","console");
                        break;

                    case "-gm":
                    case "--graphic-mode":
                        System.setProperty("loom.mode","graphical");
                        break;

                    case "-ip":
                        ip = args[++i];
                        break;

                    case "--disable-fake-mode":
                        ConfigModule.fakeIP = "null";
                        ConfigModule.fakeMac = "null";
                        ConfigModule.writeProfile();
                        logger.info("Fake IP and MAC Cleared.");
                        System.exit(0);
                        break;

                    case "--disable-auto-mode":
                        ConfigModule.autoOnlineTime = "";
                        ConfigModule.autoOfflineTime = "";
                        ConfigModule.writeProfile();
                        logger.info("Auto-mode closed.");
                        System.exit(0);
                        break;

                    case "--set-auto-mode":
                        if(args[i + 1].matches(ConfigModule.timeFormat + "\\-" + ConfigModule.timeFormat)){
                            String[] matchBuffer = args[i + 1].split("\\-");
                            ConfigModule.autoOnlineTime = matchBuffer[0];
                            ConfigModule.autoOfflineTime = matchBuffer[1];
                            if(args.length > (i+1)){
                                if(args[i + 2].matches("(both|online|offline)")) ConfigModule.autoOnlineMode = args[i + 2];
                            }
                            ConfigModule.writeProfile();
                            logger.info("Auto-mode set.");
                        }
                        System.exit(0);
                        break;

                    case "--fake-ip":
                        logger.info("Writing fake IP and Mac to profile.");
                        ConfigModule.fakeIP = args[++i];
                        if(args[++i].toLowerCase().equals("--fake-mac")){
                            ConfigModule.fakeMac = args[++i].replace(":","");
                            ConfigModule.writeProfile();
                        }
                        System.exit(0);
                        break;

                    default:
                        break;
                }
            }
        }else{
            bootGraphicMode();
        }

        logger.info("System: " + Toolbox.getSystemName());

        logger.warn(isDeveloperMode() ? "DeveloperMode on" +
                "\n\t\t!!! Warning !!!" +
                "\nDeveloper mode will record all user data(included account and password)" +
                "\nPlease remember to clear logs for protect your personal Data" : "DeveloperMode off");

        printNetworkInterface();

        if(System.getProperty("loom.mode").equals("console")){
            bootConsoleMode(ip);
        }else{
            bootGraphicMode();
        }
    }

    private static void bootConsoleMode(String ip){
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("booting");
        if(isDuplicateInstance()) System.exit(0);
        logger.info("Loom Console Mode");
        Shuttle.LoomConsole(ip);
    }

    private static void initLogger() throws IOException {
        Properties properties = new Properties();
        String configPath = "/com/shinonometn/loom/resource/configure/";
        if("t".equals(System.getProperty("loom.developerMode"))) configPath += "log4j.dev.properties";
        else configPath += "log4j.default.properties";
        properties.load(new InputStreamReader(Program.class.getResourceAsStream(configPath)));
        PropertyConfigurator.configure(properties);
    }

    private static void bootGraphicMode(){
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("booting");
        if(isDuplicateInstance()) System.exit(0);
        logger.info("Loom Graphic Mode");
        try{
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }catch (Exception e){
            logger.error(e);
        }
        new MainForm();
    }

    public static void aboutMe(){
        System.out.println(Resource.getResource().getResourceText("/com/shinonometn/loom/resource/text/aboutMe.txt"));
    }

    private static boolean isDuplicateInstance(){
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("booting");
        File _lockFile = new File("./profile/.lock");
        FileLock _wLock;

        try {
            if(!_lockFile.exists()){
                _lockFile.createNewFile();
                _lockFile.deleteOnExit();
            }
            _wLock = new FileOutputStream(_lockFile).getChannel().tryLock();
            if(_wLock == null){
                System.out.println("Not Allow more than one Loom use same profile. Program exits.");
                return true;
            }
            logger.info("Get lock success.");

        } catch (IOException e) {
            logger.error("Lock failed.");
        }
        return false;
    }

    private static void printNetworkInterface(){
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("booting");
        Vector<NetworkInterface> nf = Networks.getNetworkInterfaces(false);
        StringBuilder stringBuilder = new StringBuilder("\n\n");
        if(nf != null) {
            stringBuilder.append("Network Interfaces found:\n");
            for (NetworkInterface n : nf) {
                try {
                    stringBuilder.append(String.format("[%s]%n", n.getDisplayName())).append("\n");
                    List<InterfaceAddress> list = n.getInterfaceAddresses();
                    for (InterfaceAddress ia : list) {
                        try {
                            stringBuilder.append(ia.getAddress()).append("\n");
                        } catch (Exception e) {
                            stringBuilder.append("null\n");
                        }
                    }
                    stringBuilder.append(HexTools.byte2HexStr(n.getHardwareAddress())).append("\n");
                    //System.out.println();
                } catch (Exception e) {
                    stringBuilder.append("Null\n");
                }
                stringBuilder.append("\n\n");
            }
        }
        logger.info(stringBuilder.toString());
    }
}