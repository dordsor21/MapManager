package org.inventivetalent.mapmanager;

import org.inventivetalent.mapmanager.manager.MapManager;
import org.inventivetalent.reflection.minecraft.Minecraft;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Container class for images
 * <p>
 * Stores colors as an integer array
 */
public class ArrayImage {

	private int[] array;
	private int   width;
	private int   height;

	protected int minX = 0;
	protected int minY = 0;
	protected int maxX = 128;
	protected int maxY = 128;

	//Only used if the cache is enabled
	private Object packetData;

	private int imageType = BufferedImage.TYPE_4BYTE_ABGR;

	protected ArrayImage(int[] array) {
		this.array = array;
	}

	/**
	 * Convert a {@link BufferedImage} to an ArrayImage
	 *
	 * @param image image to convert
	 */
	public ArrayImage(BufferedImage image) {
		this.imageType = image.getType();

		this.width = image.getWidth();
		this.height = image.getHeight();
		//		int[][] intArray = ImageToMultiArray(image);
		//		int length = width * height;
		//		this.array = new int[length];
		//		for (int x = 0; x < intArray.length; x++) {
		//			for (int y = 0; y < intArray[x].length; y++) {
		//				array[y * image.getWidth() + x] = intArray[x][y];
		//			}
		//		}
		this.array = ImageToArray(image);
	}

	/**
	 * Construct an ArrayImage from raw data
	 *
	 * @param data raw image data
	 */
	public ArrayImage(int[][] data) {
		this.array = new int[data.length * data[0].length];
		this.width = data.length;
		this.height = data[0].length;
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length; y++) {
				array[y * data.length + x] = data[x][y];
			}
		}
	}

	public int[] getData() {
		return array;
	}

	public ArrayImage updateSection(int xOffset, int yOffset, BufferedImage image) {
		return updateSection(xOffset, yOffset, ImageToMultiArray(image));
	}

	public ArrayImage updateSection(int xOffset, int yOffset, int[][] intArray) {
		int[] arrayClone = new int[this.array.length];
		System.arraycopy(this.array, 0, arrayClone, 0, this.array.length);

		for (int x = 0; x < intArray.length; x++) {
			for (int y = 0; y < intArray[x].length; y++) {
				arrayClone[(y + yOffset) * intArray.length + (x + xOffset)] = intArray[x][y];
			}
		}

		ArrayImage newImage = new ArrayImage(arrayClone);
		newImage.width = this.width;
		newImage.height = this.height;

		// Section
		newImage.minX = xOffset;
		newImage.minY = yOffset;
		newImage.maxX = intArray.length;
		newImage.maxY = intArray[0].length;

		return newImage;
	}

	final boolean newerThan1_8 = Minecraft.VERSION.newerThan(Minecraft.Version.v1_8_R1);

	/**
	 * @return the generated byte data-array for the map packet
	 */
	protected Object generatePacketData() {
		if (MapManager.Options.CACHE_DATA && this.packetData != null) { return this.packetData; }

		if (MapManager.Options.TIMINGS) { TimingsHelper.startTiming("MapManager:ArrayImage:generatePacket"); }

		Object dataObject = null;
		if (newerThan1_8) {
			byte[] data = new byte[128 * 128];
			//			Arrays.fill(data, (byte) 0);
			for (int x = 0; x < 128; x++) {
				for (int y = 0; y < 128; y++) {
					data[y * 128 + x] = MapSender.matchColor(new Color(getRGB(x, y), true));
				}
			}

			dataObject = data;
		} else {// 1.7
			byte[][] dataArray = new byte[128][131];
			for (int x = 0; x < 128; x++) {
				byte[] bytes = new byte[131];

				bytes[1] = (byte) x;
				for (int y = 0; y < 128; y++) {
					bytes[y + 3] = MapSender.matchColor(getRGB(x, y));
				}

				dataArray[x] = bytes;
			}

			dataObject = dataArray;
		}

		if (MapManager.Options.TIMINGS) { TimingsHelper.stopTiming("MapManager:ArrayImage:generatePacket"); }
		if (MapManager.Options.CACHE_DATA) {
			this.packetData = dataObject;
			return this.packetData;
		} else {
			return dataObject;
		}
	}

	/**
	 * @param x x-pixel
	 * @param y y-pixel
	 * @return the RGB-value at the specified position
	 */
	public int getRGB(int x, int y) {
		return array[y * width + x];
	}

	/**
	 * @return the width of the image
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return the height of the image
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Convert this image back to a {@link BufferedImage}
	 *
	 * @return new {@link BufferedImage}
	 */
	public BufferedImage toBuffered() {
		BufferedImage image = new BufferedImage(getWidth(), getHeight(), this.imageType);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, array[y * getWidth() + x]);
			}
		}
		return image;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) { return true; }
		if (o == null || getClass() != o.getClass()) { return false; }

		ArrayImage that = (ArrayImage) o;

		if (width != that.width) { return false; }
		if (height != that.height) { return false; }
		return Arrays.equals(array, that.array);

	}

	@Override
	public int hashCode() {
		int result = array != null ? Arrays.hashCode(array) : 0;
		result = 31 * result + width;
		result = 31 * result + height;
		return result;
	}

	protected static int[][] ImageToMultiArray(BufferedImage image) {
		int[][] array = new int[image.getWidth()][image.getHeight()];
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				array[x][y] = image.getRGB(x, y);
			}
		}
		return array;
	}

	protected static int[] ImageToArray(BufferedImage image) {
		if (MapManager.Options.TIMINGS) { TimingsHelper.startTiming("MapManager:ArrayImage:ImageToArray"); }

		int[] array = new int[image.getWidth() * image.getHeight()];
		for (int x = 0; x < image.getWidth(); x++) {
			for (int y = 0; y < image.getHeight(); y++) {
				array[y * image.getWidth() + x] = image.getRGB(x, y);
			}
		}

		if (MapManager.Options.TIMINGS) { TimingsHelper.stopTiming("MapManager:ArrayImage:ImageToArray"); }
		return array;
	}

	protected static boolean ImageContentEqual(BufferedImage b1, BufferedImage b2) {
		if (b1 == null || b2 == null) { return false; }
		// if (b1.equals(b2)) return true;
		if (b1.getWidth() != b2.getWidth()) { return false; }
		if (b1.getHeight() != b2.getHeight()) { return false; }
		for (int y = 0; y < b1.getHeight(); y++) {
			for (int x = 0; x < b1.getWidth(); x++) {
				if (b1.getRGB(x, y) != b2.getRGB(x, y)) { return false; }
			}
		}
		return true;
	}

	protected static boolean ImageContentEqual(ArrayImage b1, ArrayImage b2) {
		if (b1 == null || b2 == null) { return false; }
		// if (b1.equals(b2)) return true;
		if (b1.getWidth() != b2.getWidth()) { return false; }
		if (b1.getHeight() != b2.getHeight()) { return false; }
		for (int y = 0; y < b1.getHeight(); y++) {
			for (int x = 0; x < b1.getWidth(); x++) {
				if (b1.getRGB(x, y) != b2.getRGB(x, y)) { return false; }
			}
		}
		return true;
	}
}
