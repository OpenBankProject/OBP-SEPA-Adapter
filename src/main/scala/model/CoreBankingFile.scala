package model

import java.nio.file.Path
import java.time.LocalDateTime
import java.util.UUID

import model.enums.CoreBankingFileStatus.CoreBankingFileStatus
import model.enums.CoreBankingFileType.CoreBankingFileType

case class CoreBankingFile(id: UUID,
                           name: String,
                           path: Path,
                           fileType: CoreBankingFileType,
                           status: CoreBankingFileStatus,
                           receiptDate: Option[LocalDateTime],
                           processedDate: Option[LocalDateTime],
                           numberOfTransactions: Option[Int])
