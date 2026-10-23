package forestry.api.core;

/**
 * An IErrorLogicSource uses an instance of IErrorLogic to deal with its errors.
 */
public interface IErrorLogicSource {
	IErrorLogic getErrorLogic();
}
