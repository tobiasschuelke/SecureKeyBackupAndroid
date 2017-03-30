package com.android.secret.sharing;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;

import com.android.share.sharing.R;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Uses the print functions of the android system. API level must be greater than 19.
 */
@TargetApi(Build.VERSION_CODES.KITKAT)
class QrCodePrintAdapter extends PrintDocumentAdapter {
    private PrintedPdfDocument mPdfDocument;
    private Context mContext;
    private String mName;
    private Bitmap mQrCode;
    private boolean mDrawBackup;

    public QrCodePrintAdapter(Context context, String name, Bitmap qrCode, boolean drawBackup) {
        mContext = context;
        mName = name;
        mQrCode = qrCode;
        mDrawBackup = drawBackup;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
        // Create a new PdfDocument with the requested page attributes
        mPdfDocument = new PrintedPdfDocument(mContext, newAttributes);

        // Respond to cancellation request
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        String fileName = String.format(mContext.getString(mDrawBackup ? R.string.email_subject_backup : R.string.email_subject), mName);
        fileName += ".pdf";


        PrintDocumentInfo info = new PrintDocumentInfo
                .Builder(fileName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build();

        // Content layout reflow is complete
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {
        PdfDocument.Page page = mPdfDocument.startPage(0);

        // check for cancellation
        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            mPdfDocument.close();
            mPdfDocument = null;
            return;
        }

        if (mDrawBackup) {
            drawBackup(mContext, page.getCanvas(), mQrCode, mName);
        } else {
            drawKeyPart(mContext, page.getCanvas(), mQrCode, mName);
        }

        // Rendering is complete, so page can be finalized.
        mPdfDocument.finishPage(page);

        // Write PDF document to file
        try {
            mPdfDocument.writeTo(new FileOutputStream(
                    destination.getFileDescriptor()));
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
            return;
        } finally {
            mPdfDocument.close();
            mPdfDocument = null;
        }

        PageRange[] writtenPages = new PageRange[] {new PageRange(0, 0)};
        // Signal the print framework the document is complete
        callback.onWriteFinished(writtenPages);

    }

    static void drawKeyPart(Context context, Canvas canvas, Bitmap qrCode, String contactName) {
        Resources res = context.getResources();
        String userName = DatabaseHelper.getHelper(context).getUserName();

        String headline = String.format(res.getString(R.string.printed_qr_owner), userName);
        String hints = res.getString(R.string.printed_qr_hints);

        // units are in points (1/72 of an inch)
        int top = 72;
        int leftMargin = 54;
        int lineOffset = 16;

        Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        canvas.drawText(contactName, leftMargin, top, paint);

        top += 35;
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        drawTextMultiLine(canvas, paint, headline, leftMargin, 0, top, lineOffset + 2);

        top += 40;
        int leftQrCode = (canvas.getWidth() - qrCode.getWidth()) / 2;
        canvas.drawBitmap(qrCode, leftQrCode, top, null);

        top += qrCode.getHeight() + 30;
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        drawTextMultiLine(canvas, paint, hints, leftMargin, 10, top, lineOffset);
    }

    static void drawBackup(Context context, Canvas canvas, Bitmap qrCode, String dataBackupName) {
        Resources res = context.getResources();
        String hints = res.getString(R.string.printed_qr_backup_hints);
        String restore = res.getString(R.string.printed_qr_backup_restore);

        // units are in points (1/72 of an inch)
        int top = 72;
        int leftMargin = 54;
        int lineOffset = 16;

        Paint paint = new Paint(Paint.LINEAR_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);

        String title = String.format(context.getString(R.string.printed_qr_backup_title), dataBackupName);

        top += 35;
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        drawTextMultiLine(canvas, paint, title, leftMargin, 0, top, lineOffset + 2);

        top += 40;
        int leftQrCode = (canvas.getWidth() - qrCode.getWidth()) / 2;
        canvas.drawBitmap(qrCode, leftQrCode, top, null);

        top += qrCode.getHeight() + 30;
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        top = drawTextMultiLine(canvas, paint, hints, leftMargin, 10, top, lineOffset);

        top += 30;
        drawTextMultiLine(canvas, paint, restore, leftMargin, 10, top, lineOffset);
    }

    /**
     * Draw text over multiple lines.
     *
     * @param text New lines will be drawn at every \n character.
     * @param left Left margin of text.
     * @param leftOffset Left offset margin of lines below first row.
     * @param top Top margin of text.
     * @param topOffset Offset to next line.
     * @return Top value of last line.
     */
    static int drawTextMultiLine(Canvas canvas, Paint paint, String text, int left, int leftOffset, int top, int topOffset) {
        String[] lines = text.split("\n");

        canvas.drawText(lines[0], left, top, paint);
        left += leftOffset;

        for (int i = 1; i < lines.length; i++) {
            top += topOffset;
            canvas.drawText(lines[i], left, top, paint);
        }

        return top;
    }
}
