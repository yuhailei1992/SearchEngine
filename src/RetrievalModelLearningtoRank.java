import java.util.HashMap;


public class RetrievalModelLearningtoRank extends RetrievalModel {

    /**
     * Set a retrieval model parameter.
     * @param parameterName
     * @param parametervalue
     * @return Always false because this retrieval model has no parameters.
     */
	@Override
    public boolean setParameter (String parameterName, String value) {
        return false;
    }
	/**
	 * 
	 */
	@Override
	public boolean setParameter(String parameterName, double value) {
		// TODO Auto-generated method stub
		return false;
	}
	

}
