package pasta.domain.release;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;

import pasta.archive.InvalidRebuildOptionsException;
import pasta.archive.RebuildOptions;
import pasta.domain.result.AssessmentResult;
import pasta.domain.user.PASTAUser;

/**
 * A rule that is met if the user has submitted the given assessment at least
 * once, regardless of outcome.
 * 
 * @author Joshua Stretton
 * @version 1.0
 * @since 20 Apr 2015
 */
@Entity
@Table (name = "rules_has_submitted")
public class HasSubmittedRule extends ReleaseResultsRule implements Serializable {
	private static final long serialVersionUID = 3556811612265185953L;

	@Override
	protected boolean isMet(PASTAUser user, AssessmentResult latestCompareResult) {
		return (latestCompareResult != null);
	}

	@Override
	public String getShortDescription() {
		return "Release after completing another assessment";
	}
	@Override
	public String getDescription() {
		return "This assessment will be released if the user has already "
				+ "made at least one submission to the given assessment.";
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append(": ");
		sb.append("Release if completed assessment ").append(getCompareAssessment() == null ? "null" : getCompareAssessment().getName());
		return sb.toString();
	}

	@Override
	public ReleaseRule rebuild(RebuildOptions options) throws InvalidRebuildOptionsException {
		if(options.getParentAssessment() == null) {
			throw new InvalidRebuildOptionsException("Cannot rebuild a 'has submission' rule without an assessment");
		}
		HasSubmittedRule clone = new HasSubmittedRule();
		clone.setCompareAssessment(options.getParentAssessment());
		return clone;
	}
}
