package tasks;

import play.Logger;
import cron.RuleProcessor;

/**
 * Applies the rules to a chunk of users. 
 * 
 * @author syyang
 */
public class ChunkedRuleProcessor implements Task {
    
    @Override
    public void execute(TaskContext context) throws Exception {
        Logger.info("ChunkedRuleProcessor:" + context.getTaskId() + ":finished");
    }

}
