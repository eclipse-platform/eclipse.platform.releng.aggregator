package org.eclipse.jdt.tips.user.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.json.JsonTipProvider;

public class JDTTipProvider extends JsonTipProvider {

	private static final String EMPTY = ""; //$NON-NLS-1$
	private static final String FILENAME = "jdttips.json"; //$NON-NLS-1$
	private String fUrl;
	private File fStateLocation;

	public JDTTipProvider() throws MalformedURLException {
		fUrl = System.getProperty(getID() + ".url"); //$NON-NLS-1$
		if (fUrl != null) {
			setJsonUrl(fUrl);
		} else {
			fUrl = "http://www.eclipse.org/downloads/download.php?r=1&file=/eclipse/tips/" + FILENAME; //$NON-NLS-1$
			setJsonUrl(fUrl);
		}
	}

	@Override
	public synchronized IStatus loadNewTips(IProgressMonitor pMonitor) {
		try {

			SubMonitor monitor = SubMonitor.convert(pMonitor, 1);
			monitor.setTaskName(Messages.JDTTipProvider_1);
			File localFile = fetchContent();
			try {
				setJsonUrl(localFile.toURI().toURL().toString());
			} catch (MalformedURLException e) {
				getManager().log(getStatus(Messages.JDTTipProvider_2, e));
			}
			monitor.worked(1);
			return super.loadNewTips(pMonitor);
		} catch (Exception e) {
			return getStatus(Messages.JDTTipProvider_3, e);
		}
	}

	private IStatus getStatus(String message, Exception pException) {
		return new Status(IStatus.ERROR, "org.eclipse.jdt.tips.user", message, pException); //$NON-NLS-1$
	}

	private File fetchContent() throws IOException {
		String lastModified = getLastModifiedDate();
		File timeStampFile = new File(getStateLocation(), "lastModified.txt"); //$NON-NLS-1$
		String existingVersion = getFileContent(timeStampFile);
		File contentFile = new File(getStateLocation(), FILENAME);
		if (existingVersion.equals(lastModified) && contentFile.exists()) {
			return contentFile;
		}
		storeContentLocally(contentFile);
		saveLastModifiedDate(lastModified, timeStampFile);
		return contentFile;
	}

	private void saveLastModifiedDate(String lastModified, File timeStampFile)
			throws IOException, FileNotFoundException {
		try (FileOutputStream fos = new FileOutputStream(timeStampFile)) {
			fos.write(lastModified.getBytes());
		}
	}

	private void storeContentLocally(File contentFile) throws MalformedURLException, IOException {
		URL webFile = new URL(fUrl);
		try (InputStream in = webFile.openStream()) {
			Files.copy(in, contentFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static String getFileContent(File input) throws IOException {
		if (!input.exists()) {
			input.createNewFile();
			return EMPTY;
		}
		try (FileInputStream fis = new FileInputStream(input)) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
				String result = br.readLine();
				return result == null ? EMPTY : result;
			}
		}
	}

	@Override
	public String getID() {
		return "org.eclipse.jdt.tips.user"; //$NON-NLS-1$
	}

	/**
	 * Returns the state location of the IDE tips. First the property
	 * "org.eclipse.tips.statelocation" is read. If it does not exist then the state
	 * location will be <b>${user.home}/.eclipse/org.eclipse.tips.state</b>
	 * 
	 * @return the state location file
	 * @throws IOException if something went wrong
	 */
	private File getStateLocation() throws IOException {

		if (fStateLocation != null) {
			return fStateLocation;
		}

		String stateLocation = System.getProperty(getID() + ".statelocation"); //$NON-NLS-1$
		if (stateLocation == null) {
			stateLocation = System.getProperty("user.home") + File.separator + ".eclipse" + File.separator //$NON-NLS-1$ //$NON-NLS-2$
					+ getID() + ".state"; //$NON-NLS-1$
		}
		fStateLocation = new File(stateLocation);
		if (!fStateLocation.exists()) {
			fStateLocation.mkdirs();
		}

		if (!fStateLocation.canRead() || !fStateLocation.canWrite()) {
			throw new IOException(
					MessageFormat.format(Messages.JDTTipProvider_5, fStateLocation.getAbsolutePath()));
		}
		return fStateLocation;
	}

	private String getLastModifiedDate() throws IOException {
		URL url = new URL(fUrl);
		HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
		long date = httpCon.getLastModified();
		return new Date(date).toString();
	}
}
