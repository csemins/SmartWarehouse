package bd.ac.kuet.swms.plc;

public interface JobListener {
    void jobFinished(WarehouseJob job);

    void jobAborted(WarehouseJob job);
}
