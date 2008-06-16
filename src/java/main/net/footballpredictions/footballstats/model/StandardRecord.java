// $Header: $
package net.footballpredictions.footballstats.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

/**
 * Models a single team's record for the season.  This may be their overall record
 * or it may be their home record or their away record.
 * @author Daniel Dyer
 * @since 21/12/2003
 * @version $Revision: $
 */
public final class StandardRecord extends AbstractTeamRecord
{
    private final VenueType where;

    private final List<Result> results = new ArrayList<Result>(46); // Most leagues have no more than 46 games per team.

    private final FormRecord form;

    private int won = 0;
    private int drawn = 0;
    private int lost = 0 ;
    private int scored = 0;
    private int conceded = 0;

    private final Map<SequenceType, Integer> currentSequences = new EnumMap<SequenceType, Integer>(SequenceType.class);
    private final Map<SequenceType, Integer> bestSequences = new EnumMap<SequenceType, Integer>(SequenceType.class);

    private Result biggestWin = null;
    private Result biggestDefeat = null;

    /**
     * Constructor, sets name.  All other data is added via the addResult method later.
     */
    public StandardRecord(Team team, VenueType where)
    {
        super(team);
        this.where = where;
        this.form = new FormRecord(team, where == VenueType.BOTH ? 6 : 4);
        // Intialise sequences to zero.
        for (SequenceType sequence : SequenceType.values())
        {
            currentSequences.put(sequence, 0);
            bestSequences.put(sequence, 0);
        }
    }


    public List<Result> getResults()
    {
        return results;
    }


    public void addResult(Result result)
    {
        results.add(result);
        form.addResult(result);
        updateAggregatesAndSequences(result);
    }


    public int getPlayed()
    {
        return results.size();
    }


    public int getWon()
    {
        return won;
    }


    public int getDrawn()
    {
        return drawn;
    }


    public int getLost()
    {
        return lost;
    }


    public int getScored()
    {
        return scored;
    }


    public int getConceded()
    {
        return conceded;
    }


    public int getCurrentSequence(SequenceType sequence)
    {
        return currentSequences.get(sequence);
    }


    public int getBestSequence(SequenceType sequence)
    {
        return bestSequences.get(sequence);
    }


    public String getForm()
    {
        return getFormRecord().getForm();
    }


    /**
     * @return A {@link TeamRecord} that contains only data relating to the team's current
     * form.
     */
    public FormRecord getFormRecord()
    {
        return form;
    }


    /**
     * @return The result of the most recent match played.
     */
    public Result getLatestResult()
    {
        return results.isEmpty() ? null : results.get(results.size() - 1);
    }


    /**
     * @return The {@link Result} that represents the team's biggest margin of victory achieved.
     */
    public Result getBiggestWin()
    {
        return biggestWin;
    }


    /**
     * @return The {@link Result} that represents the team's biggest margin of defeat suffered.
     */
    public Result getBiggestDefeat()
    {
        return biggestDefeat;
    }


    /**
     * Returns interesting facts about the team's form.
     */
    public List<String> getNotes()
    {
        List<String> notes = new LinkedList<String>();
        String end = " " + where.getDescription().toLowerCase() + "matches.";

        // Check unbeatean/without win sequences.
        if (getCurrentSequence(SequenceType.UNBEATEN) >= 3)
        {
            notes.add("Unbeaten in last " + getCurrentSequence(SequenceType.UNBEATEN) + end);
        }
        if (getCurrentSequence(SequenceType.NO_WIN) >= 3)
        {
            notes.add("Haven't won in last " + getCurrentSequence(SequenceType.NO_WIN) + end);
        }

        // Check win/loss sequences.
        if (getCurrentSequence(SequenceType.WINS) >= 3)
        {
            notes.add("Won last " + getCurrentSequence(SequenceType.WINS) + end);
        }
        else if (getCurrentSequence(SequenceType.DRAWS) >= 3)
        {
            notes.add("Drawn last " + getCurrentSequence(SequenceType.DRAWS) + end);
        }
        else if (getCurrentSequence(SequenceType.DEFEATS) >= 3)
        {
            notes.add("Lost last " + getCurrentSequence(SequenceType.DEFEATS) + end);
        }

        // Check cleansheet/scoring sequences.
        if (getCurrentSequence(SequenceType.GAMES_NOT_SCORED_IN) >= 3)
        {
            notes.add("Haven't scored in last " + getCurrentSequence(SequenceType.GAMES_NOT_SCORED_IN) + end);
        }
        if (getCurrentSequence(SequenceType.CLEANSHEETS) >= 3)
        {
            notes.add("Haven't conceded in last " + getCurrentSequence(SequenceType.CLEANSHEETS) + end);
        }
        if (getCurrentSequence(SequenceType.GAMES_SCORED_IN) >= 10)
        {
            notes.add("Scored in last " + getCurrentSequence(SequenceType.GAMES_SCORED_IN) + end);
        }

        return notes;
    }


    private void updateAggregatesAndSequences(Result result)
    {
        int goalsFor = result.getGoalsFor(getTeam());
        int goalsAgainst = result.getGoalsAgainst(getTeam());
        int marginOfVictory = result.getMarginOfVictory();

        // Update result aggregates/sequences.
        if (result.isDefeat(getTeam()))
        {
            lost++;

            incrementSequence(SequenceType.NO_WIN);
            incrementSequence(SequenceType.DEFEATS);
            resetSequence(SequenceType.UNBEATEN);
            resetSequence(SequenceType.WINS);
            resetSequence(SequenceType.DRAWS);

            if (biggestDefeat == null || marginOfVictory > biggestDefeat.getMarginOfVictory())
            {
                biggestDefeat = result;
            }
        }
        else
        {
            incrementSequence(SequenceType.UNBEATEN);
            resetSequence(SequenceType.DEFEATS);

            if (result.isDraw())
            {
                drawn++;

                incrementSequence(SequenceType.DRAWS);
                incrementSequence(SequenceType.NO_WIN);
                resetSequence(SequenceType.WINS);
            }
            else // Must be a win
            {
                won++;

                incrementSequence(SequenceType.WINS);
                resetSequence(SequenceType.NO_WIN);
                resetSequence(SequenceType.DRAWS);

                if (biggestWin == null || marginOfVictory > biggestWin.getMarginOfVictory())
                {
                    biggestWin = result;
                }
            }
        }

        // Update score aggregates/sequences.
        scored += goalsFor;
        conceded += goalsAgainst;
        if (goalsFor == 0)
        {
            incrementSequence(SequenceType.GAMES_NOT_SCORED_IN);
            resetSequence(SequenceType.GAMES_SCORED_IN);
        }
        else
        {
            resetSequence(SequenceType.GAMES_NOT_SCORED_IN);
            incrementSequence(SequenceType.GAMES_SCORED_IN);
        }

        if (goalsAgainst == 0)
        {
            incrementSequence(SequenceType.CLEANSHEETS);
        }
        else
        {
            resetSequence(SequenceType.CLEANSHEETS);
        }
    }


    private void incrementSequence(SequenceType sequence)
    {
        int newValue = currentSequences.get(sequence) + 1;
        currentSequences.put(sequence, newValue);
        // If the current sequence is better than the best this season, then
        // update the best this season.
        if (newValue > bestSequences.get(sequence))
        {
            bestSequences.put(sequence, newValue);
        }
    }


    private void resetSequence(SequenceType sequence)
    {
        currentSequences.put(sequence, 0);
    }


    /**
     * Over-ride equals.  Teams are equal if the names are equal.
     * No need to also over-ride {@link #hashCode()} because this
     * method is consistent with the superclass hash code.
     */
    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof StandardRecord && super.equals(obj);
    }
}
