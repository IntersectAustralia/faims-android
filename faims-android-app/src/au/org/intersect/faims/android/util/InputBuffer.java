package au.org.intersect.faims.android.util;

public class InputBuffer {
	
	public interface InputBufferListener {
		public void onFull(String inputSequence);
		public void onInput(String inputSequence);
	}

	private InputBufferListener listener;
	private int bufferSize;
	private Character endOfInput;
	private Character[] buffer;
	private int bufferIndex;
	
	public InputBuffer(int bufferSize, Character endOfInput) {
		this.bufferSize = bufferSize;
		this.endOfInput = endOfInput;
		clearBuffer();
	}
	
	public void setListener(InputBufferListener listener) {
		this.listener = listener;
	}
	
	public void addInput(Character c) {
		if (bufferSize == 0) return;
		
		if (c.equals(endOfInput)) {
			if (listener != null) {
				listener.onInput(bufferToString());
			}
			clearBuffer();
		} else {
			buffer[bufferIndex] = c;
			bufferIndex++;
			if (bufferIndex == bufferSize) {
				if (listener != null) {
					listener.onFull(bufferToString());
				}
				clearBuffer();
			}
		}
	}
	
	private String bufferToString() {
		return null;
	}
	
	private void clearBuffer() {
		buffer = new Character[bufferSize];
		bufferIndex = 0;
	}

}
