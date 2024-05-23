package selector;

import java.awt.Point;
import java.util.ListIterator;

/**
 * Models a selection tool that connects each added point with a straight line.
 */
public class PointToPointSelectionModel extends SelectionModel {

    public PointToPointSelectionModel(boolean notifyOnEdt) {
        super(notifyOnEdt);
    }

    public PointToPointSelectionModel(SelectionModel copy) {
        super(copy);
    }

    /**
     * Return a straight line segment from our last point to `p`.
     */
    @Override
    public PolyLine liveWire(Point p) {

        PolyLine line = new PolyLine(lastPoint(),p);
        return line;
        // TODO 2B: Implement this method as specified by constructing and returning a new PolyLine
        //  representing the desired line segment.  This can be done with one statement.
        //  Test immediately with `testLiveWireEmpty()`, and think about how the test might change
        //  for non-empty selections (see task 2D).

    }

    /**
     * Append a straight line segment to the current selection path connecting its end with `p`.
     */
    @Override
    protected void appendToSelection(Point p) {
        // TODO 2C: Create a line segment from the end of the previous segment (or from the starting
        //  point if this is only the 2nd point) to the current point `p`, then append that segment
        //  to the current selection path.  This can be done with one statement, similar to
        //  `liveWire()` above.
        //  Test immediately with `testAppend()` and `testFinishSelection()`.
        selection.add(new PolyLine(lastPoint(),p));
    }

    /**
     * Move the starting point of the segment of our selection with index `index` to `newPos`,
     * connecting to the end of that segment with a straight line and also connecting `newPos` to
     * the start of the previous segment (wrapping around) with a straight line (these straight
     * lines replace both previous segments).  Notify listeners that the "selection" property has
     * changed.
     */
    @Override
    public void movePoint(int index, Point newPos) {
        // Confirm that we have a closed selection and that `index` is valid
        if (state() != SelectionState.SELECTED) {
            throw new IllegalStateException("May not move point in state " + state());
        }
        if (index < 0 || index >= selection.size()) {
            throw new IllegalArgumentException("Invalid segment index " + index);
        }

        // TODO 4B: Complete the implementation of this method as specified using a `ListIterator`.
        //  You will need to replace two segments of the selection with different PolyLines, and
        //  this replacement can be done efficiently while iterating by using `ListIterator`'s
        //  `set()` method.  Think carefully about how to "wrap around" if `index` corresponds to
        //  the starting point of the selection.
        //  Reminder: If the moved point corresponds to the starting point for the selection, then
        //  you will also need to update the `start` field appropriately while avoiding rep exposure
        //  (remember that `Point` is a mutable class, so you will want to _copy_ client-provided
        //  Points rather than aliasing them).
        //  Finally, notify listeners that the "selection" property has changed (see parent class
        //  for examples).
        //  Test immediately with `testMovePointMiddle()`, and add additional tests per the
        //  corresponding task in the test suite (strongly consider writing the tests first).


        ListIterator<PolyLine> iterSelection = selection.listIterator();


        while(iterSelection.hasNext()){
            int currentIndex = iterSelection.nextIndex();
            PolyLine current = iterSelection.next();


            if(currentIndex == index){
                PolyLine newLine = new PolyLine(newPos,current.end());

                iterSelection.set(newLine);

                if(currentIndex > 0 ){
                    iterSelection.previous();
                    PolyLine previous = iterSelection.previous();
                    PolyLine newLine2 = new PolyLine(previous.start(),newPos);
                    iterSelection.set(newLine2);
                }

                break;
            }


        }

        if(index == 0 && !selection.isEmpty()){
            iterSelection = selection.listIterator(selection.size()-1);
            PolyLine lastSegment = iterSelection.next();
            PolyLine newLastSegment = new PolyLine(lastSegment.start(),newPos);
            iterSelection.set(newLastSegment);
            start = newPos;
        }

        propSupport.firePropertyChange("selection",null,selection());




    }
}
