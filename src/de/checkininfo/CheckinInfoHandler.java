package de.checkininfo;

import java.lang.reflect.Method;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class CheckinInfoHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        ISelection selection = HandlerUtil.getActiveMenuSelection(event);

        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }

        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object logEntry = structuredSelection.getFirstElement();

        if (logEntry == null) {
            return null;
        }

        try {
            // Commit-Kommentar auslesen (verschiedene Methodennamen probieren)
            String comment = tryGetString(logEntry, "getComment", "getMessage");

            // Revision auslesen
            String revision = tryGetRevision(logEntry);

            // Changed paths auslesen
            Object[] changedPaths = tryGetChangedPaths(logEntry);

            // Branch ermitteln
            String branchName = extractBranchName(changedPaths);

            // Geänderte Dateien formatieren
            String changedFiles = formatChangedFiles(changedPaths, revision);

            CheckinInfoDialog dialog = new CheckinInfoDialog(shell, branchName, comment, changedFiles);
            dialog.open();
        } catch (Exception e) {
            MessageDialog.openError(shell, "Checkin Info Fehler",
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return null;
    }

    private String tryGetString(Object obj, String... methodNames) {
        for (String name : methodNames) {
            try {
                Object result = invoke(obj, name);
                if (result != null) {
                    return result.toString();
                }
            } catch (Exception e) {
                // nächste Methode probieren
            }
        }
        return "";
    }

    private String tryGetRevision(Object logEntry) {
        try {
            Object rev = invoke(logEntry, "getRevision");
            if (rev != null) {
                // SVNRevision.getNumber() oder toString()
                try {
                    return String.valueOf(invoke(rev, "getNumber"));
                } catch (Exception e) {
                    return rev.toString();
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "?";
    }

    private Object[] tryGetChangedPaths(Object logEntry) {
        // Verschiedene Methoden probieren
        String[] methods = {"getChangedPaths", "getLogEntryChangePaths"};
        for (String name : methods) {
            try {
                Object result = invoke(logEntry, name);
                if (result instanceof Object[]) {
                    return (Object[]) result;
                }
            } catch (Exception e) {
                // nächste probieren
            }
        }
        return null;
    }

    private String formatChangedFiles(Object[] changedPaths, String revision) {
        if (changedPaths == null || changedPaths.length == 0) {
            return "(keine Dateien verfügbar)";
        }
        StringBuilder sb = new StringBuilder();
        for (Object cp : changedPaths) {
            try {
                String path = tryGetString(cp, "getPath");
                String action = "";
                try {
                    Object a = invoke(cp, "getAction");
                    action = (a != null) ? a.toString() + " " : "";
                } catch (Exception e) {
                    // ignore
                }
                sb.append(action).append(path).append(" (r").append(revision).append(")\n");
            } catch (Exception e) {
                // skip
            }
        }
        return sb.toString();
    }

    private String extractBranchName(Object[] changedPaths) {
        if (changedPaths == null || changedPaths.length == 0) {
            return "unknown";
        }
        for (Object cp : changedPaths) {
            try {
                String path = tryGetString(cp, "getPath");
                String branch = parseBranchFromPath(path);
                if (!"unknown".equals(branch)) {
                    return branch;
                }
            } catch (Exception e) {
                // skip
            }
        }
        return "unknown";
    }

    private Object invoke(Object obj, String methodName) throws Exception {
        Method method = obj.getClass().getMethod(methodName);
        return method.invoke(obj);
    }

    private static String parseBranchFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }

        int branchesIdx = path.indexOf("/branches/");
        if (branchesIdx >= 0) {
            String afterBranches = path.substring(branchesIdx + "/branches/".length());
            int slashIdx = afterBranches.indexOf('/');
            if (slashIdx > 0) {
                return afterBranches.substring(0, slashIdx);
            }
            return afterBranches;
        }

        if (path.contains("/trunk/") || path.endsWith("/trunk")) {
            return "trunk";
        }

        int tagsIdx = path.indexOf("/tags/");
        if (tagsIdx >= 0) {
            String afterTags = path.substring(tagsIdx + "/tags/".length());
            int slashIdx = afterTags.indexOf('/');
            if (slashIdx > 0) {
                return "tags/" + afterTags.substring(0, slashIdx);
            }
            return "tags/" + afterTags;
        }

        return "unknown";
    }
}
