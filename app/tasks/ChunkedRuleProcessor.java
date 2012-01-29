package tasks;

import org.apache.log4j.Logger;

import cron.RuleProcessor;

/**
 * 
 * @author syyang
 */
public class ChunkedRuleProcessor implements Task {

    private static final Logger logger = Logger.getLogger(ChunkedRuleProcessor.class);
    
    @Override
    public void execute(TaskContext context) throws Exception {
        logger.info("ChunkedRuleProcessor:" + context.getTaskId() + ":finished");
    }

}
