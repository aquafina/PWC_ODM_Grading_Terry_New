package view.classes;

import java.sql.Types;

import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import javax.faces.event.ActionEvent;

import javax.faces.event.ValueChangeEvent;

import model.AM.PwcOdmGradingTerryAMImpl;

import oracle.adf.model.BindingContext;
import oracle.adf.model.binding.DCBindingContainer;
import oracle.adf.model.binding.DCDataControl;
import oracle.adf.model.binding.DCIteratorBinding;
import oracle.adf.view.rich.component.rich.RichPopup;
import oracle.adf.view.rich.component.rich.data.RichTable;
import oracle.adf.view.rich.component.rich.input.RichInputText;

import oracle.adf.view.rich.component.rich.layout.RichPanelFormLayout;
import oracle.adf.view.rich.component.rich.output.RichOutputText;
import oracle.adf.view.rich.context.AdfFacesContext;
import oracle.adf.view.rich.event.DialogEvent;

import oracle.adf.view.rich.event.LaunchPopupEvent;

import oracle.adf.view.rich.event.ReturnPopupEvent;

import oracle.binding.BindingContainer;
import oracle.binding.OperationBinding;

import oracle.jbo.Row;
import oracle.jbo.RowSetIterator;
import oracle.jbo.ViewObject;
import oracle.jbo.server.ApplicationModuleImpl;

import oracle.wsm.common.util.CommonUtil;
import oracle.adf.view.rich.event.DialogEvent.Outcome;

import oracle.adfinternal.view.faces.model.binding.FacesCtrlLOVBinding;

import oracle.jbo.domain.Number;

import utils.system;

public class PwcGradingTerryManagedBean {
    private RichPopup completeJobPopup;
    private RichPopup returnJobPopup;
    private RichOutputText totalQuantityOT;
    private RichInputText gradeAQty;
    private RichInputText gradeBQty;
    private RichInputText gradeCQty;
    private RichInputText rewashQty;
    private RichTable gradingLinesTable;
    private RichPanelFormLayout headerForm;

    public PwcGradingTerryManagedBean() {
        super();
    }
    
    public void callExecuteWithParamsMethod(int job_id) {
        BindingContainer bindings = getBindingsCont();
        OperationBinding operationBinding = bindings.getOperationBinding("ExecuteWithParams");
        Map params =  operationBinding.getParamsMap();
        params.put("p_job_id", job_id);
        operationBinding.execute();
    }

    /*****Generic Method to Get BindingContainer**/
    public BindingContainer getBindingsCont() {
     return BindingContext.getCurrent().getCurrentBindingsEntry();
    }

    /**
     * Generic Method to execute operation
     * */
    public OperationBinding executeOperation(String operation) {
    OperationBinding createParam = getBindingsCont().getOperationBinding(operation);
    return createParam;
    }
    
    public static void showMessage(String message, int code) {

            FacesMessage.Severity s = null;
            if (code == 112) {
                s = FacesMessage.SEVERITY_ERROR;
            } else if (code == 111) {
                s = FacesMessage.SEVERITY_INFO;
            }

            FacesMessage msg = new FacesMessage(s, message, "");
            FacesContext.getCurrentInstance().addMessage(null, msg);
    }
    
    public static PwcOdmGradingTerryAMImpl getApplicationModule() {
        FacesContext fctx = FacesContext.getCurrentInstance();
        BindingContext bindingContext = BindingContext.getCurrent();
        DCDataControl dc = bindingContext.findDataControl("PwcOdmGradingTerryAMDataControl");
        return (PwcOdmGradingTerryAMImpl)dc.getDataProvider();
        }
    
    public void deleteSelectedRows(ActionEvent actionEvent) {
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject linesVO = am.findViewObject("GradingTerryLinesVO3");
        RowSetIterator rsi = linesVO.createRowSetIterator(null);
        while (rsi.next()!=null) {
            Row currRow = rsi.getCurrentRow();
            System.out.println("GdId = "+currRow.getAttribute("GdId"));
            if (currRow.getAttribute("RowSelected")!=null)
            {
                if (currRow.getAttribute("RequestStatus")!=null)
                {
                    if(currRow.getAttribute("RequestStatus").equals("S"))
                        showMessage("Completed Line(s) cannot be deleted",112);
                    else 
                    {
                        if ((Boolean)currRow.getAttribute("RowSelected")==true)
                            currRow.remove();
                    }
                }
                else
                {
                    if ((Boolean)currRow.getAttribute("RowSelected")==true)
                        currRow.remove();
                }
            }
        }
        rsi.closeRowSetIterator();
        ViewObject currHeadersVO = am.findViewObject("GradingTerryHeadersVO4");
        int job_id = Integer.parseInt(currHeadersVO.getCurrentRow().getAttribute("JobId").toString());
        am.getDBTransaction().commit();
        callExecuteWithParamsMethod(job_id);
    }
    
    public void setCompleteJobPopup(RichPopup completeJobPopup) {
        this.completeJobPopup = completeJobPopup;
    }

    public RichPopup getCompleteJobPopup() {
        return completeJobPopup;
    }
    
    public void setReturnJobPopup(RichPopup returnJobPopup) {
        this.returnJobPopup = returnJobPopup;
    }

    public RichPopup getReturnJobPopup() {
        return returnJobPopup;
    }
    
    public void completeJobAPIActionListener(ActionEvent actionEvent) {
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject currHeadersVO = am.findViewObject("GradingTerryHeadersVO4");
        Row currRow = currHeadersVO.getCurrentRow();
        int job_id = Integer.parseInt(currRow.getAttribute("JobId").toString());
        if (currRow!=null)
        {
            Double stitchingQty = Double.parseDouble(currRow.getAttribute("StitchQuantity")!=null?currRow.getAttribute("StitchQuantity").toString():"0.0");
            Double sumTotalQtyValue = Double.parseDouble(currRow.getAttribute("SumTotalQuantity")!=null?currRow.getAttribute("SumTotalQuantity").toString():"0.0");
            System.out.println("stitchingQty = "+stitchingQty);
            System.out.println("sumTotalQtyValue = "+sumTotalQtyValue);
            if (sumTotalQtyValue.compareTo(stitchingQty)==1) {
                String message =
                       "Sum of Total Quantities cannot exceed Stitching Quantity";
                showMessage(message, 112);
            }
            else
            {
                if (checkIfAnyRowSelected())
                {
                    if (isJobCompleted()) {
                        showMessage("Job already completed.", 112);
                    }
                    else
                    {
                        currHeadersVO.getCurrentRow().setAttribute("Attribute1", "S");
                        checkAndDeleteZeroRow();
                        am.getDBTransaction().commit();
                        callExecuteWithParamsMethod(job_id);
                        RichPopup.PopupHints hints = new RichPopup.PopupHints();
                        completeJobPopup.show(hints);
                    }
                }
                else showMessage("No line(s) selected", 112);
            }
        }
    }
    
    public void returnJobAPIActionListener(ActionEvent actionEvent) {
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject currHeadersVO = am.findViewObject("GradingTerryHeadersVO4");
        Row currRow = currHeadersVO.getCurrentRow();
        int job_id = Integer.parseInt(currRow.getAttribute("JobId").toString());
        if (currRow!=null)
        {
            Double stitchingQty = Double.parseDouble(currRow.getAttribute("StitchQuantity")!=null?currRow.getAttribute("StitchQuantity").toString():"0.0");
            Double sumTotalQtyValue = Double.parseDouble(currRow.getAttribute("SumTotalQuantity")!=null?currRow.getAttribute("SumTotalQuantity").toString():"0.0");
            System.out.println("stitchingQty = "+stitchingQty);
            System.out.println("sumTotalQtyValue = "+sumTotalQtyValue);
            if (sumTotalQtyValue.compareTo(stitchingQty)==1) {
                String message =
                       "Sum of Total Quantities cannot exceed Stitching Quantity";
                showMessage(message, 112);
            }
            else
            {
                if (checkIfAnyRowSelected())
                {
                    if (isJobReturned()) {
                        showMessage("Job not completed yet", 112);
                    }
                    else
                    {
                        am.getDBTransaction().commit();
                        callExecuteWithParamsMethod(job_id);
                        RichPopup.PopupHints hints = new RichPopup.PopupHints();
                        returnJobPopup.show(hints);
                    }
                }
                else showMessage("No line(s) selected", 112);
            }
        }
    }
    
    public Boolean isJobCompleted() {
        Boolean result = true;
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject gradingWaveingLinesVO = am.findViewObject("GradingTerryLinesVO3");
        RowSetIterator rsi = gradingWaveingLinesVO.createRowSetIterator(null);
        if (rsi.getAllRowsInRange().length>0) {
            while (rsi.next()!=null) {
                Row currRow = rsi.getCurrentRow();
                if (currRow.getAttribute("RequestStatus")==null) {
                    result = false;
                    break;
                }
                else if (currRow.getAttribute("RequestStatus").equals("R"))
                {
                    result = false;
                    break;
                }
            }
        }
        else return false;
        rsi.closeRowSetIterator();
        return result;
    }

    public Boolean isJobReturned() {
        Boolean result = true;
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject gradingWaveingLinesVO = am.findViewObject("GradingTerryLinesVO3");
        RowSetIterator rsi = gradingWaveingLinesVO.createRowSetIterator(null);
        while (rsi.next()!=null) {
            Row currRow = rsi.getCurrentRow();
            if ((Boolean)currRow.getAttribute("RowSelected")==Boolean.TRUE)
            {
                if (currRow.getAttribute("RequestStatus")!=null)
                {
                    if (currRow.getAttribute("RequestStatus").equals("S")) {
                        result = false;
                        break;
                    }
                }
            }
        }
        rsi.closeRowSetIterator();
        return result;
    }

    public void completeJobDialogListener(DialogEvent dialogEvent) {
        Outcome outcome = dialogEvent.getOutcome();
        if (outcome == Outcome.yes) {
            ApplicationModuleImpl am = getApplicationModule();
            ViewObject currHeadersVO = am.findViewObject("GradingTerryHeadersVO4");
            Row currRow = currHeadersVO.getCurrentRow();
            int job_id = Integer.parseInt(currRow.getAttribute("JobId").toString());
            if (currRow!=null)
            {
                checkAndDeleteZeroRow();
                String stmt = "PWC_ODM_WO_LESS_COMPL_TER_API(? " +
                    ",?" +
                    ",?" +
                    ",?" +
                    ",?" +
                    ",?" +          
                    ",?)";
                BindingContainer bindings = getBindingsCont();
                OperationBinding operationBinding = bindings.getOperationBinding("callAPIProc");
                Map params =  operationBinding.getParamsMap();
                params.put("sqlReturnType", Types.VARCHAR);
                params.put("stmt", stmt);
                params.put("requestStatus", "S");
                String result =(String) operationBinding.execute();
                    System.out.println("result = "+result);
                if (result != null) {
                    if (result.equals("SUCCESSFUL"))
                        showMessage("Job Completed Successfully!", 111);
                    else
                        showMessage(result+"", 112);
                }
            }
            else showMessage("No job found", 112);
            completeJobPopup.hide();
            callExecuteWithParamsMethod(job_id);
            AdfFacesContext.getCurrentInstance().addPartialTarget(gradingLinesTable);
        }
        else {
            completeJobPopup.hide();
        }
    }

    public void returnJobDialogListener(DialogEvent dialogEvent) {
        Outcome outcome = dialogEvent.getOutcome();
        if (outcome == Outcome.yes) {
            ApplicationModuleImpl am = getApplicationModule();
            ViewObject currHeadersVO = am.findViewObject("GradingTerryHeadersVO4");
            Row currRow = currHeadersVO.getCurrentRow();
            int job_id = Integer.parseInt(currRow.getAttribute("JobId").toString());
            if (currRow!=null)
            {
                String jobId = currRow.getAttribute("JobId").toString();
                String stmt = "PWC_ODM_WO_LESS_RET_TER_API(? " +
                    ",?" +
                    ",?" +
                    ",?" +
                    ",?" +
                    ",?" +          
                    ",?)";
                BindingContainer bindings = getBindingsCont();
                OperationBinding operationBinding = bindings.getOperationBinding("callAPIProc");
                Map params =  operationBinding.getParamsMap();
                params.put("sqlReturnType", Types.VARCHAR);
                params.put("stmt", stmt);
                params.put("requestStatus", "R");
                String result =(String) operationBinding.execute();
                    System.out.println("result = "+result);
                if (result != null) {
                    if (result.equals("SUCCESSFUL"))
                        showMessage("Line(s) Returned Successfully!", 111);
                    else
                        showMessage(result+"", 112);
                }
            }
            else showMessage("No job found", 112);
            callExecuteWithParamsMethod(job_id);
            returnJobPopup.hide();
            AdfFacesContext.getCurrentInstance().addPartialTarget(gradingLinesTable);
        }
        else {
            returnJobPopup.hide();
        }
    }
    
    public Boolean checkIfAnyRowSelected() {
        Boolean result = false;
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject gradingWaveingLinesVO = am.findViewObject("GradingTerryLinesVO3");
        RowSetIterator rsi = gradingWaveingLinesVO.createRowSetIterator(null);
        while (rsi.next()!=null) {
            Row currRow = rsi.getCurrentRow();
            if ((Boolean)currRow.getAttribute("RowSelected")==Boolean.TRUE)
            {
                result = true;
                break;
            }
        }
        rsi.closeRowSetIterator();
        return result;
    }
    
    public void deleteHeaderRow(ActionEvent actionEvent) {
        if (isJobCompleted())
            showMessage("Job cannot be deleted because it's completed", 112);
        else
        {
            ApplicationModuleImpl am = getApplicationModule();
            ViewObject currHeadersVO = am.findViewObject("GradingTerryHeadersVO4");
            ViewObject linesVO = am.findViewObject("GradingTerryLinesVO3");
            RowSetIterator rsi = linesVO.createRowSetIterator(null);
            System.out.println("lines = "+rsi.getAllRowsInRange().length);
            if (linesVO.getAllRowsInRange().length>0)
                showMessage("Delete all lines before deleting the header", 112);
            else
            {
                currHeadersVO.getCurrentRow().remove();
                am.getDBTransaction().commit();
                rsi = currHeadersVO.createRowSetIterator(null);
                Row lastRow = rsi.last();
                int lastRowIndex = rsi.getRangeIndexOf(lastRow);
                Row newRow = rsi.createRow();
                newRow.setNewRowState(Row.STATUS_INITIALIZED);
                rsi.insertRowAtRangeIndex(0, newRow); 
                rsi.setCurrentRow(newRow);      
            }
            rsi.closeRowSetIterator();
        }
    }

    private BindingContainer getBindings() {
           return BindingContext.getCurrent().getCurrentBindingsEntry();
       }

       private ViewObject getViewObject(String iterator) {
           BindingContainer bindings = getBindings();
           DCBindingContainer bc = (DCBindingContainer) bindings;
           DCIteratorBinding it = bc.findIteratorBinding(iterator);
           return it.getViewObject();
       }

    public void setTotalQuantityOT(RichOutputText totalQuantityOT) {
        this.totalQuantityOT = totalQuantityOT;
    }

    public RichOutputText getTotalQuantityOT() {
        return totalQuantityOT;
    }

    public void setGradeAQty(RichInputText gradeAQty) {
        this.gradeAQty = gradeAQty;
    }

    public RichInputText getGradeAQty() {
        return gradeAQty;
    }

    public void setGradeBQty(RichInputText gradeBQty) {
        this.gradeBQty = gradeBQty;
    }

    public RichInputText getGradeBQty() {
        return gradeBQty;
    }

    public void setGradeCQty(RichInputText gradeCQty) {
        this.gradeCQty = gradeCQty;
    }

    public RichInputText getGradeCQty() {
        return gradeCQty;
    }

    public void setRewashQty(RichInputText rewashQty) {
        this.rewashQty = rewashQty;
    }

    public RichInputText getRewashQty() {
        return rewashQty;
    }
    
    public double getTotalQty(double gradeA , double gradeB, double gradeC, double reWash){
        double result = 0;
        
        result = gradeA + gradeB + gradeC + reWash;
        return result;
    }


    public void saveChanges(ActionEvent actionEvent) {
        // Add event code here...
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject linesVO = am.findViewObject("GradingTerryLinesVO3");
        ViewObject currHeadersVO = am.findViewObject("GradingTerryHeadersVO4");
        checkAndDeleteZeroRow();
        if (currHeadersVO.getCurrentRow()!=null)
        {
            Double stitchingQty = Double.parseDouble(currHeadersVO.getCurrentRow().getAttribute("StitchQuantity")!=null?currHeadersVO.getCurrentRow().getAttribute("StitchQuantity").toString():"0.0");
            Double sumTotalQtyValue = Double.parseDouble(currHeadersVO.getCurrentRow().getAttribute("SumTotalQuantity")!=null?currHeadersVO.getCurrentRow().getAttribute("SumTotalQuantity").toString():"0.0");
            System.out.println("stitchingQty = "+stitchingQty);
            System.out.println("sumTotalQtyValue = "+sumTotalQtyValue);
            if (sumTotalQtyValue.compareTo(stitchingQty)==1) {
                String message =
                       "Sum of Total Quantities cannot exceed Stitching Quantity";
                showMessage(message, 112);
            }
            else
            {
                currHeadersVO.getCurrentRow().setAttribute("Attribute1", "S");
                int job_id = Integer.parseInt(currHeadersVO.getCurrentRow().getAttribute("JobId").toString());
                System.out.println("into else part");
                am.getDBTransaction().commit();
                callExecuteWithParamsMethod(job_id);
                AdfFacesContext.getCurrentInstance().addPartialTarget(headerForm);
                
            }
        }
        else System.out.println("header is null");
    }
    
    public void checkAndDeleteZeroRow() {
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject gradingWaveingLinesVO = am.findViewObject("GradingTerryLinesVO3");
        RowSetIterator rsi = gradingWaveingLinesVO.createRowSetIterator(null);
        while (rsi.next()!=null) {
            Row currRow = rsi.getCurrentRow();
            System.out.println("Total Qty = "+Integer.parseInt(currRow.getAttribute("TotalQuantityTransient").toString())); 
            if (Integer.parseInt(currRow.getAttribute("Gradea").toString())==0 && Integer.parseInt(currRow.getAttribute("Gradeb").toString())==0 && Integer.parseInt(currRow.getAttribute("Gradec").toString())==0 && Integer.parseInt(currRow.getAttribute("Rewash").toString())<=0)
            {
                rsi.removeCurrentRow();
            }
        }
        rsi.closeRowSetIterator();
    }
    
    public void jobLovLaunchPopupListener(LaunchPopupEvent launchPopupEvent) {
         BindingContext bctx = BindingContext.getCurrent();
         BindingContainer bindings = bctx.getCurrentBindingsEntry();
         FacesCtrlLOVBinding lov =
         (FacesCtrlLOVBinding)bindings.get("Job");
         String wcl = "wip_entity_id not in (select  nvl(job_id,0) from PWC_ODM_GRADING_TERRY_HEADERS)";
         lov.getListIterBinding().getViewObject().setWhereClause(wcl);
        }

    public void returnJobLovPopupListener(ReturnPopupEvent returnPopupEvent) {
        BindingContext bctx = BindingContext.getCurrent();
        BindingContainer bindings = bctx.getCurrentBindingsEntry();
        FacesCtrlLOVBinding lov =
        (FacesCtrlLOVBinding)bindings.get("Job");
        lov.getListIterBinding().getViewObject().setWhereClause(null);
    }

    public void setGradingLinesTable(RichTable gradingLinesTable) {
        this.gradingLinesTable = gradingLinesTable;
    }

    public RichTable getGradingLinesTable() {
        return gradingLinesTable;
    }


    public void createInsertHeaderAction(ActionEvent actionEvent) {
        // Add event code here...
        BindingContainer bindings = getBindingsCont();
        OperationBinding operationBinding = bindings.getOperationBinding("Rollback");
        operationBinding.execute();
        operationBinding = bindings.getOperationBinding("CreateInsert");
        operationBinding.execute();
    }

    public void gradeAValueChangeListener(ValueChangeEvent valueChangeEvent) {
        // Add event code here...
        if (valueChangeEvent.getNewValue()==null)
            gradeAQty.setValue(new Number(0));
    }
    
    public void gradeBValueChangeListener(ValueChangeEvent valueChangeEvent) {
        // Add event code here...
        if (valueChangeEvent.getNewValue()==null)
            gradeBQty.setValue(new Number(0));
    }
    
    public void gradeCValueChangeListener(ValueChangeEvent valueChangeEvent) {
        // Add event code here...
        if (valueChangeEvent.getNewValue()==null)
            gradeCQty.setValue(new Number(0));
    }
    
    public void rewashValueChangeListener(ValueChangeEvent valueChangeEvent) {
        // Add event code here...
        if (valueChangeEvent.getNewValue()==null)
            rewashQty.setValue(new Number(0));
    }

    public void setHeaderForm(RichPanelFormLayout headerForm) {
        this.headerForm = headerForm;
    }

    public RichPanelFormLayout getHeaderForm() {
        return headerForm;
    }

    public void backAction(ActionEvent actionEvent) {
        // Add event code here...
        checkAndDeleteZeroRow();
        BindingContainer bindings = getBindingsCont();
        OperationBinding operationBinding = bindings.getOperationBinding("Rollback");
        bindings = getBindingsCont();
        operationBinding = bindings.getOperationBinding("ExecuteWithParams");
        Map params =  operationBinding.getParamsMap();
        params.put("p_job_id", null);
        operationBinding.execute();
    }

    public void editAction(ActionEvent actionEvent) {
        // Add event code here...
        // setCurrentRowWithKey
        ApplicationModuleImpl am = getApplicationModule();
        ViewObject currHeadersVO = am.findViewObject("GradingTerryHeadersVO4");
        int job_id = Integer.parseInt(currHeadersVO.getCurrentRow().getAttribute("JobId").toString());
        BindingContainer bindings = getBindingsCont();
        OperationBinding operationBinding = bindings.getOperationBinding("setCurrentRowWithKey");
        bindings = getBindingsCont();
        operationBinding = bindings.getOperationBinding("ExecuteWithParams");
        Map params =  operationBinding.getParamsMap();
        params.put("p_job_id", job_id);
        operationBinding.execute();
    }
}
