package dk.bec.unittest.becut.debugscript.model;

import java.util.ArrayList;
import java.util.List;

public class If implements Statement {

	private Conditional conditional;
	private List<Statement> body;

	public If(Conditional conditional, List<Statement> body) {
		this.conditional = conditional;
		this.body = body;
	}

	public If(Conditional conditional, Statement body) {
		this.conditional = conditional;
		this.body = new ArrayList<Statement>();
		this.body.add(body);
	}

	@Override
	public String generate() {
		String result = "";
		if (body.isEmpty()) {
			return result;
		}
		result += "       IF " + conditional.generate() + " THEN\n";
		if (body.size() == 1) {
			result += body.get(0).generate();
		} else {
			for (Statement statement : body) {
				result += statement.generate();
			}

		}
		result += "       END-IF;\n";
		return result;
	}

}
