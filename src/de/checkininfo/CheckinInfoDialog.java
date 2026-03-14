package de.checkininfo;

import java.nio.charset.StandardCharsets;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CheckinInfoDialog extends TitleAreaDialog {

    private final String branchName;
    private final String comment;
    private final String changedFiles;

    private Text branchText;
    private Text commentText;
    private Text changelogText;
    private Text testsText;
    private Text testdestText;

    public CheckinInfoDialog(Shell parentShell, String branchName, String comment, String changedFiles) {
        super(parentShell);
        this.branchName = branchName != null ? branchName : "";
        this.comment = comment != null ? comment : "";
        this.changedFiles = changedFiles != null ? changedFiles : "";
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Checkin Info");
        newShell.setMinimumSize(600, 550);
    }

    @Override
    public void create() {
        super.create();
        setTitle("Checkin Info");
        setMessage("Checkin-Informationen zusammenstellen und in die Zwischenablage kopieren.");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 10;
        layout.marginHeight = 10;
        container.setLayout(layout);

        // Branch
        Label branchLabel = new Label(container, SWT.NONE);
        branchLabel.setText("Branch:");
        branchLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        branchText = new Text(container, SWT.BORDER);
        branchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        branchText.setText(branchName);

        // Checkin-Kommentar
        Label commentLabel = new Label(container, SWT.NONE);
        commentLabel.setText("Kommentar:");
        commentLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        commentText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData commentData = new GridData(SWT.FILL, SWT.FILL, true, true);
        commentData.heightHint = 80;
        commentText.setLayoutData(commentData);
        commentText.setText(comment);

        // Changelog (geänderte Dateien)
        Label changelogLabel = new Label(container, SWT.NONE);
        changelogLabel.setText("Changelog:");
        changelogLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        changelogText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        GridData changelogData = new GridData(SWT.FILL, SWT.FILL, true, true);
        changelogData.heightHint = 100;
        changelogText.setLayoutData(changelogData);
        changelogText.setText(changedFiles);

        // Tests
        Label testsLabel = new Label(container, SWT.NONE);
        testsLabel.setText("Tests:");
        testsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        testsText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData testsData = new GridData(SWT.FILL, SWT.FILL, true, true);
        testsData.heightHint = 80;
        testsText.setLayoutData(testsData);

        // Testdest (Wo sind die Tests erfolgt?)
        Label testdestLabel = new Label(container, SWT.NONE);
        testdestLabel.setText("Testdest:");
        testdestLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        testdestText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData testdestData = new GridData(SWT.FILL, SWT.FILL, true, true);
        testdestData.heightHint = 60;
        testdestText.setLayoutData(testdestData);

        return area;
    }

    private static final int COPY_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, COPY_BUTTON_ID, "In Clipboard kopieren", false);
        createButton(parent, IDialogConstants.OK_ID, "Schliessen", true);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == COPY_BUTTON_ID) {
            copyToClipboard();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    private void copyToClipboard() {
        String template = loadTemplate();
        if (template == null) {
            return;
        }
        String text = template
            .replace("{VERSION}", branchText.getText())
            .replace("{PATCHNOTES}", commentText.getText())
            .replace("{TESTS}", testsText.getText())
            .replace("{TESTDEST}", testdestText.getText())
            .replace("{CHANGELOG}", changelogText.getText());
        Clipboard clipboard = new Clipboard(getShell().getDisplay());
        try {
            clipboard.setContents(
                    new Object[]{text},
                    new Transfer[]{TextTransfer.getInstance()}
            );
            setMessage("In die Zwischenablage kopiert!");
        } finally {
            clipboard.dispose();
        }
    }

    private String loadTemplate() {
        try (java.io.InputStream is = getClass().getResourceAsStream("Erledigt_Kommentar.txt")) {
            if (is == null) {
                setErrorMessage("Template-Datei nicht gefunden (Erledigt_Kommentar.txt)");
                return null;
            }
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            setErrorMessage("Fehler beim Laden des Templates: " + e.getMessage());
            return null;
        }
    }
}
