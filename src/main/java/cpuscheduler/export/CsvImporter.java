package cpuscheduler.export;

import cpuscheduler.model.ProcessModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Robust CSV parser and validator for importing workload definitions.
 * Correctly parses quoted tokens containing commas and whitespace.
 * Validates header schema, numeric ranges, and checks for duplicate process IDs.
 */
public class CsvImporter {

    public static List<ProcessModel> importFromFile(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("Workload file not found: " + file.getAbsolutePath());
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return parseCsv(sb.toString());
        }
    }

    public static List<ProcessModel> parseCsv(String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV content is empty.");
        }

        List<List<String>> rows = parseCsvRows(csvContent);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV contains no data rows.");
        }

        // Identify header row
        List<String> header = rows.get(0);
        int colPid = -1, colAt = -1, colBt = -1, colPr = -1;

        for (int i = 0; i < header.size(); i++) {
            String col = header.get(i).trim().toLowerCase(Locale.US).replace("_", " ");
            if (col.equals("process") || col.equals("pid") || col.equals("process id") || col.equals("name") || col.equals("id")) {
                colPid = i;
            } else if (col.equals("arrival") || col.equals("arrival time") || col.equals("at")) {
                colAt = i;
            } else if (col.equals("burst") || col.equals("burst time") || col.equals("bt") || col.equals("cpu burst")) {
                colBt = i;
            } else if (col.equals("priority") || col.equals("pr") || col.equals("prio")) {
                colPr = i;
            }
        }

        if (colPid < 0 || colAt < 0 || colBt < 0) {
            throw new IllegalArgumentException("CSV header must include at least Process (PID), Arrival Time, and Burst Time columns. Found: " + header);
        }

        List<ProcessModel> processes = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.isEmpty() || (row.size() == 1 && row.get(0).trim().isEmpty())) {
                continue; // Skip blank line
            }

            int lineNum = r + 1;
            if (row.size() <= Math.max(colPid, Math.max(colAt, colBt))) {
                throw new IllegalArgumentException("Line " + lineNum + ": Missing required column values (got " + row.size() + " columns).");
            }

            String pid = row.get(colPid).trim();
            if (pid.isEmpty()) {
                throw new IllegalArgumentException("Line " + lineNum + ": Process ID cannot be empty.");
            }
            if (!seenIds.add(pid)) {
                throw new IllegalArgumentException("Line " + lineNum + ": Duplicate process ID '" + pid + "'.");
            }

            int at;
            try {
                at = Integer.parseInt(row.get(colAt).trim());
                if (at < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Line " + lineNum + ": Arrival time must be a non-negative integer (found '" + row.get(colAt) + "').");
            }

            int bt;
            try {
                bt = Integer.parseInt(row.get(colBt).trim());
                if (bt <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Line " + lineNum + ": Burst time must be a positive integer (found '" + row.get(colBt) + "').");
            }

            int pr = 1; // Default priority if omitted
            if (colPr >= 0 && colPr < row.size() && !row.get(colPr).trim().isEmpty()) {
                try {
                    pr = Integer.parseInt(row.get(colPr).trim());
                    if (pr < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Line " + lineNum + ": Priority must be a non-negative integer (found '" + row.get(colPr) + "').");
                }
            }

            processes.add(new ProcessModel(pid, at, bt, pr, processes.size()));
        }

        if (processes.isEmpty()) {
            throw new IllegalArgumentException("No valid processes found in CSV.");
        }

        return processes;
    }

    public static List<List<String>> parseCsvRows(String input) {
        List<List<String>> result = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < input.length() && input.charAt(i + 1) == '"') {
                        currentToken.append('"');
                        i++; // Skip escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentToken.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    currentRow.add(currentToken.toString());
                    currentToken.setLength(0);
                } else if (c == '\r') {
                    // Check if CRLF
                    if (i + 1 < input.length() && input.charAt(i + 1) == '\n') {
                        i++;
                    }
                    currentRow.add(currentToken.toString());
                    currentToken.setLength(0);
                    result.add(currentRow);
                    currentRow = new ArrayList<>();
                } else if (c == '\n') {
                    currentRow.add(currentToken.toString());
                    currentToken.setLength(0);
                    result.add(currentRow);
                    currentRow = new ArrayList<>();
                } else {
                    currentToken.append(c);
                }
            }
        }

        if (currentToken.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentToken.toString());
            result.add(currentRow);
        }

        return result;
    }
}
