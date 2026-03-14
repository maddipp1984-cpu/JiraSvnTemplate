package de.checkininfo;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import java.lang.reflect.Method;

public class CheckinInfoActionDelegate implements IObjectActionDelegate {

    private Shell shell;
    private ISelection selection;

    @Override
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        shell = targetPart.getSite().getShell();
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        this.selection = selection;
    }

    @Override
    public void run(IAction action) {
        if (!(selection instanceof IStructuredSelection)) {
            return;
        }

        IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        Object logEntry = structuredSelection.getFirstElement();

        if (logEntry == null) {
            return;
        }

        try {
            String comment = tryGetString(logEntry, "getComment", "getMessage");
            String revision = tryGetRevision(logEntry);
            Object[] changedPaths = tryGetChangedPaths(logEntry);
            String branchName = extractBranchName(changedPaths);
            String changedFiles = formatChangedFiles(changedPaths, revision);

            CheckinInfoDialog dialog = new CheckinInfoDialog(shell, branchName, comment, changedFiles);
            dialog.open();
        } catch (Exception e) {
            MessageDialog.openError(shell, "Checkin Info Fehler",
                e.getClass().getSimpleName() + ": " + e.getMessage());
        }
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

        // VER-Struktur: .../VER/4.5.0/ModulName/...
        int verIdx = path.indexOf("/VER/");
        if (verIdx >= 0) {
            String afterVer = path.substring(verIdx + "/VER/".length());
            String[] parts = afterVer.split("/");
            if (parts.length >= 2) {
                return parts[0] + " / " + parts[1];
            }
            if (parts.length == 1 && !parts[0].isEmpty()) {
                return parts[0];
            }
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
