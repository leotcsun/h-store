package edu.brown.benchmark.auctionmark.procedures;

import org.apache.log4j.Logger;
import org.voltdb.ProcInfo;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.VoltTable.ColumnInfo;
import org.voltdb.types.TimestampType;

import edu.brown.benchmark.auctionmark.AuctionMarkConstants;

@ProcInfo (
    partitionInfo = "USER.U_ID: 1",
    singlePartition = true
)
public class NewFeedback extends VoltProcedure{
    private static final Logger LOG = Logger.getLogger(NewFeedback.class);
    
    // -----------------------------------------------------------------
    // STATIC MEMBERS
    // -----------------------------------------------------------------
    
    private static final ColumnInfo[] RESULT_COLS = {
        new VoltTable.ColumnInfo("if_id", VoltType.BIGINT),
    };
    
    // -----------------------------------------------------------------
    // STATEMENTS
    // -----------------------------------------------------------------
    
    public final SQLStmt selectMaxFeedback = new SQLStmt(
        "SELECT MAX(uf_id) " + 
        "  FROM " + AuctionMarkConstants.TABLENAME_USER_FEEDBACK + " " + 
        " WHERE uf_i_id = ? AND uf_u_id = ?"
    );
	
    public final SQLStmt insertFeedback = new SQLStmt(
        "INSERT INTO " + AuctionMarkConstants.TABLENAME_USER_FEEDBACK + "( " +
        	"uf_id," +
        	"uf_u_id," +
        	"uf_i_id," +
        	"uf_i_u_id," +
        	"uf_from_id," +
        	"uf_rating," +
        	"uf_date," +
        	"uf_sattr0" +
        ") VALUES (" +
            "?," + // UF_ID
            "?," + // UF_U_ID
            "?," + // UF_I_ID
            "?," + // UF_I_U_ID
            "?," + // UF_FROM_ID
            "?," + // UF_RATING
            "?," + // UF_DATE
            "?"  + // UF_SATTR0
        ")"
    );
    
    public final SQLStmt updateUser = new SQLStmt(
        "UPDATE " + AuctionMarkConstants.TABLENAME_USER + " " +
           "SET u_rating = u_rating + ? " + 
        " WHERE u_id = ?"
    );
    
    // -----------------------------------------------------------------
    // RUN METHOD
    // -----------------------------------------------------------------
    
    public VoltTable run(long i_id, long seller_id, long buyer_id, long rating, String comment) {
        final boolean debug = LOG.isDebugEnabled();
        if (debug)
            LOG.debug("NewFeedback::: selecting max feedback");

        // Set comment_id
        voltQueueSQL(selectMaxFeedback, i_id, seller_id);
        VoltTable[] results = voltExecuteSQL();
        assert (1 == results.length);
        long if_id = -1;
        if (0 == results[0].getRowCount()) {
            if_id = 0;
        } else {
            results[0].advanceRow();
            if_id = results[0].getLong(0) + 1;
        }

        if (debug)
            LOG.debug("NewFeedback::: if_id = " + if_id);

        voltQueueSQL(insertFeedback, if_id, seller_id, i_id, seller_id, buyer_id, rating, new TimestampType(), comment);
        voltQueueSQL(updateUser, rating, seller_id);
        voltExecuteSQL();

        if (debug)
            LOG.debug("NewFeedback::: feedback inserted ");

        // Return new if_id
        VoltTable ret = new VoltTable(RESULT_COLS);
        ret.addRow(if_id);
        return ret;
    }
}