package dk.bec.unittest.becut.debugscript.model;

public class Comment implements DebugEntity, Statement {

	private String commentText;

	public Comment(String commentText) {
		super();
		this.commentText = commentText;
	}

	public String getCommentText() {
		return commentText;
	}

	public void setCommentText(String commentText) {
		this.commentText = commentText;
	}

	@Override
	public String generate() {
		return "      * " + commentText;
	}

}
