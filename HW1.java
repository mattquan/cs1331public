import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.Arrays;
public class PgnReader {

    public static void main(String[] args) {
        String game = fileContent(args[0]);
        //System.out.println(fileContent(args[0]));
        System.out.println(tagValue("Event", game));
        System.out.println(tagValue("Site", game));
        System.out.println(tagValue("Date", game));
        System.out.println(tagValue("Round", game));
        System.out.println(tagValue("White", game));
        System.out.println(tagValue("Black", game));
        System.out.println(tagValue("Result", game));
        System.out.printf("Final Position: %s%n", finalPosition(game));

    }

    /**
     * Find the tagName tag pair in a PGN game and return its value.
     *
     * @see http://www.saremba.de/chessgml/standards/pgn/pgn-complete.htm
     *
     * @param tagName the name of the tag whose value you want
     * @param game a `String` containing the PGN text of a chess game
     * @return the value in the named tag pair
     */
    public static String tagValue(String tagName, String game) {

        int indexOfTag = game.indexOf("[" + tagName);
        if (indexOfTag == -1) {
            return tagName + ": NOT GIVEN";
        }

        //else, we found it
        //find the content within the quotations

        int indexOfFirstQuote = game.indexOf("\"", indexOfTag);
        int indexOfSecondQuote = game.indexOf("\"", indexOfFirstQuote + 1);

        String value = game.substring(indexOfFirstQuote + 1,
            indexOfSecondQuote);

        return tagName + ": " + value;
    }

    /**
     * Play out the moves in game and return a String with the game's
     * final position in Forsyth-Edwards Notation (FEN).
     *
     * @see http://www.saremba.de/chessgml/standards/pgn/pgn-complete.htm#c16.1
     *
     * @param game a `Strring` containing a PGN-formatted chess game or opening
     * @return the game's final position in FEN.
     */
    public static String finalPosition(String game) {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";
        String[][] chessboard = convertFenTo2DArray(fen);
        //printArray(chessboard);
        //let's determine which piece was moved
        int maxTurn = getMaxTurn(game);
        //System.out.printf("maxTurn: %d%n", maxTurn);
        for (int turnNumber = 1; turnNumber <= maxTurn; turnNumber++) {

            //System.out.printf("turnNumber: %s%n", turnNumber);

            String[] turnData = returnHalfTurn(game, turnNumber);
            for (int i = 0; i < 2; i++) {
                String halfTurnCode = turnData[i];
                boolean isWhiteTurn = (i == 0) ? true : false;
                if (!turnData[i].equals("")) {
                    String[] halfTurnParsedData =
                        returnDataFromHalfTurnCode(halfTurnCode);
                    String piece = halfTurnParsedData[0];
                    String newCoordinate = halfTurnParsedData[1];
                    String typeOfMove = halfTurnParsedData[2];
                    boolean isPawnCapture = false;
                    //System.out.printf("typeOfMove: %s%n", typeOfMove);
                    if (!(typeOfMove.equals("kingside castle")
                        || typeOfMove.equals("queenside castle")
                        || typeOfMove.equals("not a move!"))) {

                        String posOfPrevPlacement = getPrevCoordinate(piece,
                            newCoordinate, chessboard, isWhiteTurn);

                        if (typeOfMove.equals("simple")) {

                            /*System.out.printf("%s moved %s --> %s%n", piece,
                                posOfPrevPlacement, newCoordinate);*/
                            updateChessboard(chessboard, piece,
                                posOfPrevPlacement, newCoordinate, isWhiteTurn,
                                isPawnCapture);

                        } else if (typeOfMove.equals("capture")) {

                            /*System.out.printf(
                                "%s captured someone. %s --> %s%n", piece,
                                posOfPrevPlacement, newCoordinate);*/

                            if ((piece.length() == 2)
                                && (piece.charAt(0) == 'P')) {
                            //was a pawn capture
                                isPawnCapture = true;
                            }
                            updateChessboard(chessboard, piece,
                                posOfPrevPlacement, newCoordinate, isWhiteTurn,
                                isPawnCapture);
                        }

                        if (piece.substring(0, 1).equals("P")
                            && (Integer.parseInt(
                                newCoordinate.substring(1, 2)) == 8
                                    || Integer.parseInt(
                                        newCoordinate.substring(1, 2)) == 1)) {
                            //System.out.println("pawn promotion");
                            pawnPromotion(newCoordinate, halfTurnParsedData[3],
                                isWhiteTurn, chessboard);
                        }

                    } else if (typeOfMove.equals("kingside castle")) {
                        if (isWhiteTurn) {
                            updateChessboard(chessboard, "K", "e1", "g1",
                                isWhiteTurn, isPawnCapture);
                            updateChessboard(chessboard, "R", "h1", "f1",
                                isWhiteTurn, isPawnCapture);
                        } else {
                            updateChessboard(chessboard, "K", "e8", "g8",
                                isWhiteTurn, isPawnCapture);
                            updateChessboard(chessboard, "R", "h8", "f8",
                                isWhiteTurn, isPawnCapture);
                        }
                    } else if (typeOfMove.equals("queenside castle")) {
                        if (isWhiteTurn) {
                            updateChessboard(chessboard, "K", "e1", "c1",
                                isWhiteTurn, isPawnCapture);
                            updateChessboard(chessboard, "R", "a1", "d1",
                                isWhiteTurn, isPawnCapture);
                        } else {
                            updateChessboard(chessboard, "K", "e8", "c8",
                                isWhiteTurn, isPawnCapture);
                            updateChessboard(chessboard, "R", "a8", "d8",
                                isWhiteTurn, isPawnCapture);
                        }
                    }

                }
            }
        }


        return convertChessboardToFen(chessboard);
    }

    public static void pawnPromotion(String newCoordinate, String piece,
        boolean isWhiteTurn, String[][] chessboard) {

        piece = (!isWhiteTurn) ? piece.toLowerCase() : piece;
        char newRank = newCoordinate.charAt(0);
        int newFile = Integer.parseInt(newCoordinate.substring(1));
        chessboard[8 - newFile][newRank - 'a'] = piece;
        //System.out.println("Updated chessboard:");
        //printArray(chessboard);

    }

    public static String convertChessboardToFen(String[][] chessboard) {
        String fen = "";
        for (int i = 0; i < chessboard.length; i++) {
            for (int j = 0; j < chessboard[0].length; j++) {
                fen += chessboard[i][j];
            }
            fen += "/";
        }

        //System.out.printf("fenDirty:%s%n", fen);
        //clean up
        //gets rid of extra dash
        fen = fen.substring(0, fen.length() - 1);
        //convert ones into numbers
        for (int i = 0; i < fen.length(); i++) {
            if (Character.isDigit(fen.charAt(i))) {
                int counter = 0;
                int startingIndex = i;
                while (i < fen.length() && Character.isDigit(fen.charAt(i))) {
                    i++;
                    counter++;
                }
                fen = fen.substring(0, startingIndex) + counter
                    + fen.substring(i);
                i = i - (counter - 1);
            }
        }
        return fen;

    }

    public static void updateChessboard(String[][] chessboard, String piece,
        String prevCoor, String newCoor, boolean isWhiteTurn,
        boolean isPawnCapture) {

        piece = piece.substring(0, 1);
        piece = (!isWhiteTurn) ? piece.toLowerCase() : piece;
        char prevRank = prevCoor.charAt(0);
        int prevFile = Integer.parseInt(prevCoor.substring(1));
        char newRank = newCoor.charAt(0);
        int newFile = Integer.parseInt(newCoor.substring(1));
        //System.out.printf("elementPrev at %c%d: %s%n", prevRank, prevFile,
            //chessboard[8 - prevFile][prevRank - 'a']);
        //if it's white turn, look for the one below
        //if it's black turn, look for the one above
        //check if the captured spot is empty
        if (isPawnCapture && chessboard[8 - newFile][newRank - 'a'].equals(
            "1")) {

            chessboard[8 - prevFile][prevRank - 'a'] = "1";
            //System.out.printf("elementPrevNew: %s%n",
                //chessboard[8 - prevFile][prevRank - 'a']);
            chessboard[8 - newFile][newRank - 'a'] = piece;
            chessboard[8 - prevFile][newRank - 'a'] = "1";

        } else {
            chessboard[8 - prevFile][prevRank - 'a'] = "1";
            //System.out.printf("elementPrevNew: %s%n",
                //chessboard[8 - prevFile][prevRank - 'a']);
            chessboard[8 - newFile][newRank - 'a'] = piece;

        }
        //System.out.println("Updated chessboard:");
        //printArray(chessboard);

    }


    public static String[] convertFENStringRowToArray(String fen) {
        String[] row = new String[8];
        int arrayPointer = 0;
        //String element = fen.substring(0, 1);
        //space is the ending for the last row
        //System.out.printf("fen check:%s%n", fen);
        for (int i = 0; i < fen.length(); i++) {
            String element = fen.substring(i, i + 1);
            if (Character.isDigit(element.charAt(0))) {
                for (int j = 0; j < Integer.parseInt(element); j++) {
                    row[arrayPointer] = "1";
                    arrayPointer++;
                }

            } else {
                //System.out.printf("i: %d | arrayPointer: %d%%n",
                    //i, arrayPointer);
                row[arrayPointer] = element;
                arrayPointer++;

            }

            //System.out.println("row: " + Arrays.toString(row));
            //System.out.printf("element: %s%n", element);
        }
        return row;

    }



    public static String[][] convertFenTo2DArray(String fen) {
        String[][] chessboard = new String[8][8];
        for (int i = 0; i < 8; i++) {
            int indexOfSlash = fen.indexOf("/");
            String babyFen = (indexOfSlash != -1)
                ? fen.substring(0, indexOfSlash) : fen;
            //System.out.printf("babyFen: %s%n", babyFen);

            chessboard[i] = convertFENStringRowToArray(babyFen);

            fen = fen.substring(indexOfSlash + 1);
        }
        return chessboard;
    }

    /*public static void printArray(String[][] matrix) {
        for (String[] row : matrix)
            System.out.println(Arrays.toString(row));
    }*/

    public static String findPawn(char rank, int file, String[][] chessboard,
        String pawn, int colorMultiplier) {

        //System.out.printf("Looking for: %s%n", pawn);

        if (checkIfRankFileIsLegit(rank, file + (colorMultiplier) * 1)
            && pieceAt(rank, file + (colorMultiplier) * 1, chessboard).equals(
                pawn)) {

            return rank + String.valueOf((file + (colorMultiplier) * 1));

        } else if (Character.isDigit(pieceAt(rank, file + (colorMultiplier) * 1,
            chessboard).charAt(0))) {

            if (checkIfRankFileIsLegit(rank, file + (colorMultiplier) * 2)
                && pieceAt(rank,
                    file + (colorMultiplier) * 2, chessboard).equals(pawn)) {

                //System.out.println("Was a pawn that moved up two");
                return rank + String.valueOf((file + (colorMultiplier) * 2));
            } else {
                return "IMPOSSIBLE PAWN MOVE";
            }
        } else {
            return "IMPOSSIBLE PAWN MOVE";
        }
    }

    public static String findKnight(char rank, int file, String[][] chessboard,
        String knight) {

        if (checkIfRankFileIsLegit((char) (rank - 1), file + 2)
            && pieceAt((char) (rank - 1), file + 2,
                chessboard).equals(knight)) {

            //System.out.println("was a knight that moved up two, left one");
            return String.valueOf((char) (rank - 1)) + String.valueOf(file + 2);

        } else if (checkIfRankFileIsLegit((char) (rank + 1), file + 2)
            && pieceAt((char) (rank + 1), file + 2,
                chessboard).equals(knight)) {

            //System.out.println("was a knight that moved up two, right one");
            return String.valueOf((char) (rank + 1)) + String.valueOf(file + 2);

        } else if (checkIfRankFileIsLegit((char) (rank + 1), file - 2)
            && pieceAt((char) (rank + 1), file - 2,
                chessboard).equals(knight)) {

            //System.out.println("was a knight that moved down two, right 1");
            return String.valueOf((char) (rank + 1)) + String.valueOf(file - 2);

        } else if (checkIfRankFileIsLegit((char) (rank - 1), file - 2)
            && pieceAt((char) (rank - 1), file - 2,
                chessboard).equals(knight)) {

            //System.out.println("was a knight that moved down 2, left one");
            return String.valueOf((char) (rank - 1)) + String.valueOf(file - 2);

        } else if (checkIfRankFileIsLegit((char) (rank + 2), file + 1)
            && pieceAt((char) (rank + 2), file + 1,
                chessboard).equals(knight)) {

            //System.out.println("was a knight that moved up 1, right 2");
            return String.valueOf((char) (rank + 2)) + String.valueOf(file + 1);

        } else if (checkIfRankFileIsLegit((char) (rank + 2), file - 1)
            && pieceAt((char) (rank + 2), file - 1,
                chessboard).equals(knight)) {

            //System.out.println("was a knight that moved down one, right 2");
            return String.valueOf((char) (rank + 2)) + String.valueOf(file - 1);

        } else if (checkIfRankFileIsLegit((char) (rank - 2), file - 1)
            && pieceAt((char) (rank - 2) , file - 1,
                chessboard).equals(knight)) {

            //System.out.println("was a knight that moved down one, left 2");
            return String.valueOf((char) (rank - 2)) + String.valueOf(file - 1);

        } else if (checkIfRankFileIsLegit((char) (rank - 2), file + 1)
            && pieceAt((char) (rank - 2), file + 1,
                chessboard) .equals(knight)) {

           //System.out.println("was a knight that moved up one, left two");
            return String.valueOf((char) (rank - 2)) + String.valueOf(file + 1);

        } else {
            return "IMPOSSIBLE KNIGHT MOVE";
        }
    }

    public static String findBishop(char rank, int file, String[][] chessboard,
        String bishop) {
        boolean foundAPiece = false;
        for (int i = 1; checkIfRankFileIsLegit((char) (rank - i), file - i)
            && !foundAPiece; i++) {
            String pieceAt = pieceAt((char) (rank - i), file - i, chessboard);

            if (!Character.isDigit(pieceAt.charAt(0))) {

                foundAPiece = true;

                if (pieceAt.equals(bishop)) {

                    return String.valueOf((char) (rank - i))
                        + String.valueOf(file - i);
                }
            }
        }
        foundAPiece = false;
        for (int i = 1; checkIfRankFileIsLegit((char) (rank - i), file + i)
            && !foundAPiece; i++) {

            String pieceAt = pieceAt((char) (rank - i), file + i, chessboard);

            if (!Character.isDigit(pieceAt.charAt(0))) {

                foundAPiece = true;

                if (pieceAt.equals(bishop)) {

                    return String.valueOf((char) (rank - i))
                        + String.valueOf(file + i);
                }
            }
        }
        foundAPiece = false;
        for (int i = 1; (checkIfRankFileIsLegit((char) (rank + i), file - i)
            && !foundAPiece); i++) {

            String pieceAt = pieceAt((char) (rank + i), file - i, chessboard);
            if (!Character.isDigit(pieceAt.charAt(0))) {

                //System.out.println("triggered");
                foundAPiece = true;

                if (pieceAt.equals(bishop)) {

                    return String.valueOf((char) (rank + i))
                        + String.valueOf(file - i);
                }
            }
        }
        foundAPiece = false;

        for (int i = 1; checkIfRankFileIsLegit((char) (rank + i), file + i)
            && !foundAPiece; i++) {
            String pieceAt = pieceAt((char) (rank + i), file + i, chessboard);
            if (!Character.isDigit(pieceAt.charAt(0))) {

                foundAPiece = true;

                if (pieceAt.equals(bishop)) {

                    return String.valueOf((char) (rank + i))
                        + String.valueOf(file + i);
                }
            }
        }

        return "IMPOSSIBLE BISHOP MOVE";
    }


    public static String getPrevCoordinate(String piece, String targetPos,
        String[][] chessboard, boolean isWhiteTurn) {

        int colorMultiplier = isWhiteTurn ? -1 : 1;

        String[] whiteSet = {"P", "N", "B", "R", "Q", "K"};
        String[] blackSet = new String[whiteSet.length];
        for (int i = 0; i < blackSet.length; i++) {
            blackSet[i] = whiteSet[i].toLowerCase();
        }

        String[] pieces = (isWhiteTurn) ? whiteSet : blackSet;


        char rank = targetPos.charAt(0);
        int file = Integer.parseInt(targetPos.substring(1));


        if (piece.equals("P")) {

            return findPawn(rank, file, chessboard, pieces[0], colorMultiplier);

        } else if (piece.equals("N")) {

            return findKnight(rank, file, chessboard, pieces[1]);

        } else if (piece.equals("B")) {

            return findBishop(rank, file, chessboard, pieces[2]);

        } else if ((piece.length() == 2) && (piece.charAt(0) == 'P')) {
            //was a pawn capture
            rank = piece.charAt(1);
            piece = piece.substring(0, 1);
            //System.out.printf("rank:%c%n", rank);
            return findPawnCapture(rank, chessboard, pieces[0]);
        } else if (piece.equals("R")) {

            return findRook(rank, file, chessboard, pieces[3]);
        } else if (piece.equals("Q")) {
            if (findRook(rank, file, chessboard, pieces[4]).equals(
                "IMPOSSIBLE ROOK MOVE")) {

                /*System.out.printf("findRook:%s%n", findRook(rank, file,
                    chessboard, pieces[4]).equals("IMPOSSIBLE ROOK MOVE"));*/
                return findBishop(rank, file, chessboard, pieces[4]);
            }
            return findRook(rank, file, chessboard, pieces[4]);
        } else if (piece.equals("K")) {
            return findKing(rank, file, chessboard, pieces[5]);
        }

        return "UNKNOWN CASE";

    }
    public static String findKing(char rank, int file, String[][] chessboard,
        String king) {
        if (checkIfRankFileIsLegit((char) (rank - 1), file)
            && pieceAt((char) (rank - 1), file, chessboard).equals(king)) {

            return String.valueOf((char) (rank - 1)) + String.valueOf(file);

        } else if (checkIfRankFileIsLegit((char) (rank - 1), file - 1)
            && pieceAt((char) (rank - 1), file - 1, chessboard).equals(king)) {

            return String.valueOf((char) (rank - 1)) + String.valueOf(file - 1);

        } else if (checkIfRankFileIsLegit((char) (rank - 1), file + 1)
            && pieceAt((char) (rank - 1), file + 1 , chessboard).equals(king)) {

            return String.valueOf((char) (rank - 1)) + String.valueOf(file + 1);

        } else if (checkIfRankFileIsLegit((char) (rank), file + 1)
            && pieceAt((char) (rank), file + 1 , chessboard).equals(king)) {

            return String.valueOf((char) (rank)) + String.valueOf(file + 1);

        } else if (checkIfRankFileIsLegit((char) (rank), file - 1)
            && pieceAt((char) (rank), file - 1 , chessboard).equals(king)) {

            return String.valueOf((char) (rank)) + String.valueOf(file - 1);

        } else if (checkIfRankFileIsLegit((char) (rank + 1), file + 1)
            && pieceAt((char) (rank + 1), file + 1 , chessboard).equals(king)) {

            return String.valueOf((char) (rank + 1)) + String.valueOf(file + 1);

        } else if (checkIfRankFileIsLegit((char) (rank + 1), file - 1)
            && pieceAt((char) (rank + 1), file - 1 , chessboard).equals(king)) {

            return String.valueOf((char) (rank + 1)) + String.valueOf(file - 1);

        } else if (checkIfRankFileIsLegit((char) (rank + 1), file)
            && pieceAt((char) (rank + 1), file, chessboard).equals(king)) {

            return String.valueOf((char) (rank + 1)) + String.valueOf(file);

        }

        return "IMPOSSIBLE KING MOVE";

    }


    public static String findRook(char rank, int file, String[][] chessboard,
        String rook) {
        boolean foundAPiece = false;
        for (int i = 1; checkIfRankFileIsLegit((char) (rank - i), file)
            && !foundAPiece; i++) {
            String pieceAt = pieceAt((char) (rank - i), file, chessboard);

            if (!Character.isDigit(pieceAt.charAt(0))) {

                foundAPiece = true;

                if (pieceAt.equals(rook)) {

                    return String.valueOf((char) (rank - i))
                        + String.valueOf(file);
                }
            }
        }
        foundAPiece = false;
        for (int i = 1; checkIfRankFileIsLegit((char) (rank + i), file)
            && !foundAPiece; i++) {
            String pieceAt = pieceAt((char) (rank + i), file, chessboard);

            if (!Character.isDigit(pieceAt.charAt(0))) {

                foundAPiece = true;

                if (pieceAt.equals(rook)) {

                    return String.valueOf((char) (rank + i))
                        + String.valueOf(file);
                }
            }
        }
        foundAPiece = false;
        for (int i = 1; checkIfRankFileIsLegit((char) (rank), file - i)
            && !foundAPiece; i++) {

            String pieceAt = pieceAt((char) (rank), file - i, chessboard);

            if (!Character.isDigit(pieceAt.charAt(0))) {

                foundAPiece = true;

                if (pieceAt.equals(rook)) {

                    return String.valueOf((char) (rank))
                        + String.valueOf(file - i);
                }
            }
        }
        foundAPiece = false;
        for (int i = 1; checkIfRankFileIsLegit((char) (rank), file + i)
            && !foundAPiece; i++) {

            String pieceAt = pieceAt((char) (rank), file + i, chessboard);

            if (!Character.isDigit(pieceAt.charAt(0))) {

                foundAPiece = true;

                if (pieceAt.equals(rook)) {

                    return String.valueOf((char) (rank))
                        + String.valueOf(file + i);
                }
            }
        }
        return "IMPOSSIBLE ROOK MOVE";
    }

    public static String findPawnCapture(char rank, String[][] chessboard,
        String pawn) {

        for (int i = 0; i < chessboard.length; i++) {
            if (chessboard[i][rank - 'a'].equals(pawn)) {
                return String.valueOf(rank) + String.valueOf(8 - i);
            }
        }
        return "IMPOSSIBLE PAWN CAPTURE";
    }

    public static boolean checkIfRankFileIsLegit(char rank, int file) {
        //System.out.printf("checkingRankFile%c%d%n", rank, file);
        //System.out.printf("return: %b", ((rank <= 'h') && (file <= 8)
            //&& (rank >= 'a') && (file >= 1)));
        return ((rank <= 'h') && (file <= 8) && (rank >= 'a') && (file >= 1));
    }

    public static String pieceAt(char rank, int file, String[][] chessboard) {
        //System.out.printf("pieceAt %c%d: %s%n", rank, file,
            //chessboard[8 - file][rank - 'a']);
        return chessboard[8 - file][rank - 'a'];

    }

    public static int getMaxTurn(String game) {
        int pointerForDot = game.length() - 1;
        while (pointerForDot >= 0 && !game.substring(pointerForDot,
            pointerForDot + 1).equals(".")) {

            pointerForDot--;
        }
        int pointerForPreceedingSpace = pointerForDot;
        while (pointerForPreceedingSpace >= 0
            && !game.substring(pointerForPreceedingSpace,
                pointerForPreceedingSpace + 1).equals(" ")) {

            pointerForPreceedingSpace--;
        }
        /*System.out.printf("maxTurn:%d%n", Integer.parseInt(game
            .substring(pointerForPreceedingSpace + 1, pointerForDot)));*/
        return Integer.parseInt(game.substring(pointerForPreceedingSpace + 1,
            pointerForDot));
    }

    public static String[] returnHalfTurn(String game, int turnNumber) {
        //where 0th element is the first half of turn
        //and 1st element is the second half of turn
        //generateHalfTurnCode
        int indexOfFirstHalfOfTurnCode = game.indexOf(turnNumber + ".") + 2
            + String.valueOf(turnNumber).length();

        //System.out.println("ind"+game.charAt(indexOfFirstHalfOfTurnCode));

        //keep going until there is a space
        int indexOfSpace = game.indexOf(" ", indexOfFirstHalfOfTurnCode);
        //System.out.printf("indexOfSpace:%d%n", indexOfSpace);
        int indexOfSecondSpace = game.indexOf(" ", indexOfSpace + 1);
        String firstHalfTurn;
        String secondHalfTurn;
        if (indexOfSpace != -1) {
            firstHalfTurn =
                game.substring(indexOfFirstHalfOfTurnCode, indexOfSpace);
            if (indexOfSecondSpace == -1) {
                secondHalfTurn = game.substring(indexOfSpace + 1);
            } else {
                secondHalfTurn =
                game.substring(indexOfSpace + 1,
                    game.indexOf(" ", indexOfSpace + 1));
            }

        } else {
            firstHalfTurn =
                game.substring(indexOfFirstHalfOfTurnCode);
            secondHalfTurn = "";
        }

        String[] data = new String[2];
        data[0] = firstHalfTurn;
        data[1] = secondHalfTurn;

        /*System.out.printf(
        "turnNumber:%d | firstHalf: %s | secondHalf: %s |%n", turnNumber,
        firstHalfTurn, secondHalfTurn);*/

        return data;
    }

    public static String[] returnDataFromHalfTurnCode(String halfTurnCode) {
        //where 0th element is the piece moved
        //and 1st element is the coordinate moved to
        //2nd element is the type of move
        //3rd element exists only if there is a pawn promotion

        String[] data = new String[4];

        if (halfTurnCode.charAt(0) != 'O') {

            //check if it's a score
            if (Character.isUpperCase(halfTurnCode.charAt(0))) {
                data[0] = halfTurnCode.substring(0, 1);
                data[1] = halfTurnCode.substring(1, 3);

            } else {

                data[0] = "P";
                data[1] = halfTurnCode;
            }

            data[2] = "simple";

            if (Character.isDigit(halfTurnCode.charAt(0))) {

                data[2] = "not a move!";
                data[1] = null;
                data[0] = null;

            }

        } else {

            if (halfTurnCode.length() == 5) {

                data[2] = "queenside castle";

            } else if (halfTurnCode.length() == 3) {

                data[2] = "kingside castle";

            }
        }

        if (halfTurnCode.contains("x")) {

            data[2] = "capture";
            if (data[0].equals("P")) {
                data[0] += halfTurnCode.substring(0, 1);
            }
            data[1] = halfTurnCode.substring(halfTurnCode.indexOf("x") + 1,
                halfTurnCode.indexOf("x") + 1 + 2);

        }

        if (halfTurnCode.contains("=")) {
            data[3] = halfTurnCode.substring(halfTurnCode.indexOf("=") + 1,
                halfTurnCode.indexOf("=") + 2);
        }
        return data;
    }

    public static String getRidOfTags(String game) {
        int indexOfFirstMove = game.indexOf("1.");
        return game.substring(indexOfFirstMove);
    }

    /**
     * Reads the file named by path and returns its content as a String.
     *
     * @param path the relative or abolute path of the file to read
     * @return a String containing the content of the file
     */
    public static String fileContent(String path) {
        Path file = Paths.get(path);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                // Add the \n that's removed by readline()
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
            System.exit(1);
        }
        return sb.toString();
    }

}