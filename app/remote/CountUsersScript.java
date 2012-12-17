package remote;

import org.joda.time.DateTime;

import play.Logger;

import com.google.appengine.api.datastore.Query.FilterOperator;

import models.DatastoreUtil;
import models.User;

public class CountUsersScript extends RemoteScript {
    @Override
    public void innerRun() {
        Logger.info("Users last week %d",
                    DatastoreUtil.count(User.all().addFilter("lastSync",
                                                             FilterOperator.GREATER_THAN_OR_EQUAL,
                                                             new DateTime().minusWeeks(1).toDate())));
        Logger.info("Users last month %d",
                    DatastoreUtil.count(User.all().addFilter("lastSync",
                                                             FilterOperator.GREATER_THAN_OR_EQUAL,
                                                             new DateTime().minusMonths(1).toDate())));
        Logger.info("Users last 6 months %d",
                    DatastoreUtil.count(User.all().addFilter("lastSync",
                                                             FilterOperator.GREATER_THAN_OR_EQUAL,
                                                             new DateTime().minusMonths(6).toDate())));
    }

    public static void main(String[] args) {
        new CountUsersScript().run();
    }
}
