import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class JarSearch {

    private HashMap<String, HashSet<String>> packageMap;
    private HashMap<String, HashSet<String>> earExpansion;
    private HashSet<String> jarFoundList;
    private boolean isJarSearch = false;
    private String searchString = "";
    private String earToExpand = "";
    private boolean includeAppInf = false;

    private int execType;

    int numPackages = 0;
    static String util = "earjarsearch";
    static Options options;
    static boolean progressBar = false;

    public JarSearch() {
        packageMap = new HashMap<String, HashSet<String>>();
        jarFoundList = new HashSet<String>();
        earExpansion = new HashMap<String, HashSet<String>>();
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    public boolean getIsJarSearch() {
        return isJarSearch;
    }

    public void setIsJarSearch(boolean isJarSearch) {
        this.isJarSearch = isJarSearch;
    }

    public String getEarToExpand() {
        return earToExpand;
    }

    public void setEarToExpand(String earToExpand) {
        this.earToExpand = earToExpand;
    }

    private boolean jarFoundListHelper(String packageContent) {
        jarFoundList.contains(packageContent);
        for (String j : jarFoundList) {
            if (packageContent.toLowerCase().indexOf(j) >= 0) {
                return true;
            }
        }
        return false;
    }

    /*
    Method input will always be a jar file.
     */
    public void expandEar(String earContent) {
        earContent = getClassOrJarName(earContent);
        if (earContent == null || earContent.equalsIgnoreCase("")) {
            return;
        }

        int app_inf = earContent.indexOf("app-inf");
        String cleanJar = getCleanClassJarName(earContent);
        cleanJar = cleanJar + ".jar";

        if (app_inf != -1 && includeAppInf) {
            String op = executeCommand("jar tvf " + cleanJar);
            cleanJar = "APP-INF/" + cleanJar;
            String[] jarContents = op.split("\n");
            updateHash(earExpansion, cleanJar, null);

            for (String j : jarContents) {
                j = getClassOrJarName(j);
                if (j == null || j.equalsIgnoreCase("")) {
                    continue;
                }
                updateHash(earExpansion, cleanJar, j);
            }
        } else if (app_inf == -1) {
            String op = executeCommand("jar tvf " + cleanJar);
            String[] jarContents = op.split("\n");
            updateHash(packageMap, cleanJar, null);
            for (String j : jarContents) {
                j = getClassOrJarName(j);
                if (j == null || j.equalsIgnoreCase("")) {
                    continue;
                }
                updateHash(earExpansion, cleanJar, j);
            }
        }
    }

    public void printEarContents(String ear) {
        System.out.println("--------------");
        System.out.println("EAR: " + ear + ".ear");

        List<String> packageMapSorted = new ArrayList(new TreeSet(earExpansion.keySet()));
        for (String packageMapKey : packageMapSorted) {
            List<String> packageMapValueList = new ArrayList(new TreeSet(earExpansion.get(packageMapKey)));
            boolean first = true;
            for (String v : packageMapValueList) {
                if (first) {
                    first = false;
                    System.out.println("  --" + packageMapKey);
                }
                System.out.println("\t|--" + v);
            }
        }
    }

    public void searchByString(String packageName) {
        String op = executeCommand("jar tvf " + packageName);
        String[] packageContents = op.split("\n");
        for (String packageContent : packageContents) {
            packageContent = getClassOrJarName(packageContent);
            if (packageContent == null || packageContent.equalsIgnoreCase("")) {
                continue;
            }
            if ((packageName.endsWith("jar") || packageName.endsWith("sar")) && packageContent.toLowerCase().indexOf(searchString) != -1) {
                updateHash(packageMap, packageName, packageContent);
                jarFoundList.add(packageName.toLowerCase());
            }
            boolean jarSearch = isJarSearch && packageContent.toLowerCase().indexOf(searchString) != -1;
            if (packageName.endsWith("ear") && (jarFoundListHelper(packageContent) || jarSearch)) {
                updateHash(packageMap, packageName, packageContent);
            }
        }
    }

    private void updateHash(HashMap<String, HashSet<String>> map, String key, String value) {
        if (!map.containsKey(key)) {
            map.put(key, new HashSet<String>());
        }
        if (value != null && !value.isEmpty()) {
            HashSet<String> hs = map.get(key);
            hs.add(value);
            map.put(key, hs);
        }
    }

    public void printSearchResult() {
        if (!isJarSearch) {
            System.out.println("--------------");
            System.out.println("JARs that contain the class are:");
            printHelper("jar");
        }
        System.out.println("--------------");
        System.out.println("EARs that contain the above JARs are:");
        printHelper("ear");
    }

    public void printHelper(String selector) {
        List<String> packageMapSorted = new ArrayList(new TreeSet(packageMap.keySet()));
        boolean found = false;
        for (String packageMapKey : packageMapSorted) {
            if (packageMapKey.endsWith(selector)) {
                found = true;
                List<String> packageMapValueList = new ArrayList(new TreeSet(packageMap.get(packageMapKey)));
                boolean first = true;
                for (String v : packageMapValueList) {
                    if (first) {
                        first = false;
                        System.out.println("  --" + packageMapKey);
                    }
                    System.out.println("\t|--" + v);
                }
            }
        }
        if (!found) {
            System.out.println("NO PACKAGES FOUND.");
        }
    }

    public String getClassOrJarName(String line) {
        String[] str = line.split(" ");
        int app_inf = line.indexOf("APP-INF");
        if ((app_inf == -1 || includeAppInf) && (str[str.length - 1].endsWith("jar") || str[str.length - 1].endsWith("sar") || str[str.length - 1].endsWith("class"))) {
            return str[str.length - 1];
        } else {
            return null;
        }
    }

    public static String executeCommand(String[] cmd) {

        String op = null;
        Process p;
        try {
            p = Runtime.getRuntime().exec(cmd);
            op = executeCommandHelper(p);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return op;
    }

    private static String executeCommandHelper(Process p) throws Exception {
        StringBuilder op = new StringBuilder();
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = "";
        while ((line = reader.readLine()) != null) {
            op.append(line + "\n");
        }
        return op.toString();
    }

    public static String executeCommand(String cmd) {

        String op = null;
        Process p;
        try {
            p = Runtime.getRuntime().exec(cmd);
            op = executeCommandHelper(p);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return op;
    }

    public static void displayVersion() {
        System.out.println(util + " version: 1.0");
    }

    public static void displayHelp() {
        System.out.println("\nUtility to perform (case insensitive) search for within packages.\n");
        displayVersion();
        System.out.println("\nExample usages:\n" +
                util + " -c oms \t\tDisplays all jars with any class with 'oms' in its class name. Also displays ears that have above jars.\n\n" +
                util + " -ac oms \t\tDisplays all jars with any class with 'oms' in its class name. Includes APP-INF jars.\n\n" +
                util + " -j umsservice \tDisplays all ears that contains any jar with 'omsservice' in its name.\n\n" +
                util + " -aj umsservice \tDisplays all ears that contains any jar with 'omsservice' in its name. Includes APP-INF jars.\n\n" +
                util + " -e umsservice.ear => Displays all jars+classes in given ear.\n"
        );
//        HelpFormatter formatter = new HelpFormatter();
//        formatter.printHelp("Options: \n", options);


    }

    public static void abort() {
        System.out.println("ERROR! \nInvalid usage. Please see available options shown below (also viewed with: " + util + " -h\n");
        displayHelp();
        System.exit(0);
    }

    public static String getCleanClassJarName(String name) {
        name = name.toLowerCase();
        int lastIndex = name.lastIndexOf('.');
        if (lastIndex != -1) {
            name = name.substring(0, lastIndex);
        }
        lastIndex = name.lastIndexOf('/');
        if (lastIndex != -1) {
            name = name.substring(lastIndex + 1, name.length());
        }
        return name;
    }

    public static void main(String[] args) {

        options = new Options();
        options.addOption("c", true, "(class) the class name to search for in the packages");
        options.addOption("a", false, "(appinf) when searching, includes jars in APP-INF folder");
        options.addOption("j", true, "(jar) the jar name to search for in the ears");
        options.addOption("e", true, "(ear) Expand an ear and show the jars + classes.");
        options.addOption("h", false, "(help) displays this help message");
        options.addOption("p", false, "(progress) displays the progress bar");
        options.addOption("v", false, "(version) displays the version number");

//        TreeMap<String, String> options = new TreeMap<String, String>();
//        options.put("c", "(class) the class name to search for in the packages");
//        options.put("a", "(appinf) when searching, includes jars in APP-INF folder");
//        options.put("j", "(jar) the jar name to search for in the ears");
//        options.put("e", "(ear) Expand an ear and show the jars + classes.");
//        options.put("h", "(help) displays this help message");
//        options.put("p", "(progress) displays the progress bar");
//        options.put("v", "(version) displays the version number");

        if (args[0].indexOf('-') == -1) {
            System.out.println("ERROR. Requires argument. Try help: earjarsearch -h");
            System.exit(0);
        }

        /*
        c
        j
        e
        ac
        aj
        ae
         */

        if (args[0].indexOf('h') >= 0) {
            displayHelp();
            System.exit(0);
        }

        if (args[0].indexOf('v') >= 0) {
            displayVersion();
            System.exit(0);
        }

//        CommandLineParser parser = new DefaultParser();
//        try {
//            CommandLine cmd = parser.parse(options, args);
//        } catch (ParseException e) {
//            System.out.println(e.getMessage());
//        }

        boolean classOpt = false, earOpt = false, jarOpt = false, appinf = false;

        if (args[0].indexOf('c') >= 0) {
            classOpt = true;
        }

        if (args[0].indexOf('j') >= 0) {
            jarOpt = true;
        }

        if (args[0].indexOf('e') >= 0) {
            earOpt = true;
        }

        if (args[0].indexOf('a') >= 0) {
            appinf = true;
        }

        if ((classOpt && earOpt) || (jarOpt && earOpt) || (classOpt && jarOpt)) {
            System.out.println("ERROR! \nOptions class/ear/jar are mutually exclusive. It cannot be applied together");
            System.exit(0);
        }

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("p") || cmd.hasOption("progress")) {
                progressBar = true;
                System.out.println(progressBar);
            }
            String classname = "";
            String earname = "";
            String jarname = "";


            System.out.println("Working Directory = " + System.getProperty("user.dir"));

            String op;
            List<String> packages = new ArrayList<String>();

            JarSearch js = new JarSearch();
            if (cmd.hasOption("a") || cmd.hasOption("appinf")) {
                js.includeAppInf = true;
            }

            if (cmd.hasOption("c") || cmd.hasOption("class")) {
                if (cmd.hasOption("c"))
                    classname = cmd.getOptionValue("c");
                else
                    classname = cmd.getOptionValue("class");
//                cmd.getOptionValue
                classOpt = true;
                classname = getCleanClassJarName(classname);
                System.out.println("Searching by class: " + classname);
                js.setSearchString(classname);
                String ls = "ls";
                op = executeCommand(ls);
                packages = Arrays.asList(op.split("\n"));
            } else if (cmd.hasOption("e") || cmd.hasOption("ear")) {
                if (cmd.hasOption("e")) {
                    earname = cmd.getOptionValue("e");
                } else {
                    earname = cmd.getOptionValue("ear");
                }
                earname = getCleanClassJarName(earname);
                System.out.println("Expanding EAR: " + earname);
                earOpt = true;
                String cmd1 = "jar tvf " + earname + ".ear";
                String earContents = executeCommand(cmd1);
                packages = Arrays.asList(earContents.split("\n"));
            } else if (cmd.hasOption("j") || cmd.hasOption("jar")) {
                if (cmd.hasOption("j")) {
                    jarname = cmd.getOptionValue("j");
                } else {
                    jarname = cmd.getOptionValue("jar");
                }
                jarname = getCleanClassJarName(jarname);
                System.out.println("Searching by JAR: " + jarname);
                jarOpt = true;
                js.setSearchString(jarname);
                js.setIsJarSearch(true);

                String[] lsEar = {
                        "/bin/sh",
                        "-c",
                        "ls *ear"
//						"ls | grep -i *.ear"
                };
//				String lsEar = "ls *ear";
                op = executeCommand(lsEar);
                packages = Arrays.asList(op.split("\n"));
            }

            if (packages.size() == 0 || packages.get(0).equalsIgnoreCase("")) {
                System.out.println("ERROR: Could not find package.");
                System.out.println("ERROR: Invalid search. Package list is empty.\nPlease recheck the folder in which this script is run.");
                System.exit(0);
            }
            js.numPackages = packages.size();
            System.out.println("Total packages searching: " + packages.size());

            if (classname.isEmpty() && jarname.isEmpty() && earname.isEmpty()) {
                System.out.println("ERROR! \nSearch parameters are empty. Cannot perform a null search.");
                abort();
            }

            int count = 0;

            System.out.println("Include APP-INF folder jars during ear search? " + js.includeAppInf);

            if (classOpt) {
//				System.out.println("Searching packages that contain class: " + classname);
                for (String p : packages) {
                    if (p.endsWith("jar") || p.endsWith("sar")) {
                        p = p.toLowerCase();
                        js.searchByString(p);
                        printProgress(++count, js.numPackages);
                    }
                }
                for (String p : packages) {
                    if (p.endsWith("ear")) {
                        p = p.toLowerCase();
                        js.searchByString(p);
                        printProgress(++count, js.numPackages);
                    }
                }
                System.out.println();
                js.printSearchResult();
            } else if (jarOpt) {
                for (String p : packages) {
                    if (p.endsWith("ear")) {
                        p = p.toLowerCase();
                        js.searchByString(p);
                        printProgress(++count, js.numPackages);
                    }
                }
                System.out.println();
                js.printSearchResult();
            } else if (earOpt) {
                for (String p : packages) {
                    p = p.toLowerCase();
                    js.expandEar(p);
//					printProgress(++count, js.numPackages);
                }
//				System.out.println();
                js.printEarContents(earname);
            }


            /*
            StackTraceElement[] callingFrame = Thread.currentThread().getStackTrace();
        logger.info("frenzy TradeInfo setfinal: " + isFinal + " Trade: " + toString() + " Stack trace = \n" + Arrays.toString(callingFrame));
             */
        } catch (ParseException e) {
            System.out.println(e.getMessage());
//            abort();
        }
    }

    private static void printProgress(int current, int total) {
        int progress = (int) Math.ceil((current * 100.0) / total);
        if (progressBar) {
            char[] progressBar = new char[100];
            for (int i = 0; i < progress; i++) {
                progressBar[i] = '=';
            }
            for (int i = progress + 1; i < progressBar.length; i++) {
                progressBar[i] = '.';
            }
            String pBar = new String(progressBar);
            System.out.print("\rSearch Progress: [" + pBar + "] " + progress + "%");
        } else {
            System.out.print("\rSearch Progress: " + progress + "%");
        }
    }

}
// todo make only one argument since there is no package search filters
// TODO search all ears that contain a jar - use has
//todo include app-inf also in ears
